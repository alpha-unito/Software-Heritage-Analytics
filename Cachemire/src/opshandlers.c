/* ***************************************************************************
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License version 3 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 *  License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 ****************************************************************************
 */
/* Author: Massimo Torquati <massimo.torquati@unipi.it>
 * 
 */

#define _GNU_SOURCE 
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <ctype.h>
#include <assert.h>
#include <stdlib.h>
#include <time.h>
#include <sys/select.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include <stats.h>
#include <conn.h>
#include <util.h>
#include <cachemire.h>

#define Min(a,b) ((a)<(b))?(a):(b)

// builds a string allocating the needed size
static inline char *createPath(const char *dir, const char *key, int extrasize) {
  int sz = strlen(dir) + 1 + KEY_SIZE + extrasize + 1;
  char *tmpfilename = calloc(sz, 1);
  assert(tmpfilename);
  strcpy(tmpfilename, dir);
  tmpfilename[strlen(dir)] = '/';
  memcpy(&tmpfilename[strlen(dir)+1], key, KEY_SIZE);
  return tmpfilename;
}
// create a tempory "unique" file name
static inline int createTempFile(const char *dir, const char *key, char **out) {  
  *out = createPath(dir, key, strlen(".XXXXXX"));
  strcat(*out, ".XXXXXX");
  int fd;
  if ((fd=mkstemp(*out)) == -1) {
    perror("mkstemp");
    fprintf(stderr, "Error: cannot create temporary file %s\n", *out);
    free(*out);
  }
  return fd;
}
// it answers to a client that the requested key file is not available
// if str is not NULL, str is sent to the client
static inline int cacheMiss(int fd, const char *str) {
  rpl_hdr_t rep;
  setFAIL(rep, str);
  if (writen(fd, rep, sizeof(rep)) == -1) {
    perror("writen");
    return -1;
  }
  if (str) {
    if (writen(fd, (char*)str, strlen(str)+1) <= 0) {
      perror("writen");
      return -1;
    }
  }
  return 0;
}
// it answers to a client that the requested key file has been found
static inline int cacheHit(int fd, void *mappedfile, size_t size) {
  rpl_hdr_t rep;
  setOK(rep);
  setSize(rep, size);
  if (writen(fd, rep, sizeof(rep)) == -1) {
    perror("writen");
    fprintf(stderr, "FATAL ERROR writing reply OK\n");
    return -1;
  }
  if (writen(fd, mappedfile, size) <= 0) {
    perror("writen");
    fprintf(stderr, "FATAL ERROR writing reply OK\n");
    return -1;
  }
  return 0;
}
// the best (non-portable) solution would be to use sendfile
static inline int sendFile(int fd, FILE* fp, size_t size) {
  const int chunksize=8192;
  char buf[chunksize];
  int ret = 0;
  for (;;) {
    size_t len = fread (buf, 1, sizeof(buf), fp);
    if (len == 0) {
      ret = feof (fp);
      break;
    }
    if (writen(fd, buf, len) <= 0) {
      perror("writen");
      fprintf(stderr, "FATAL ERROR writing a chunk in sendFile\n");
      return -1;
    }
  }
  return (ret?0:-1);
}
#if !defined(CACHEMIRE_USE_MMAP)
static inline int cacheHitSendFile(int fd, FILE* fp, size_t size) {
  rpl_hdr_t rep;
  setOK(rep);
  setSize(rep, size);
  if (writen(fd, rep, sizeof(rep)) == -1) {
    perror("writen");
    fprintf(stderr, "FATAL ERROR writing reply OK\n");
    return -1;
  }
  if (sendFile(fd, fp, size)==-1) {
    fprintf(stderr, "FATAL ERROR sendFile\n");
    return -1;
  }
  return 0;  
}
#endif 
// OK answer
static inline int replyOK(int fd) {
  rpl_hdr_t rep;
  setOK(rep);
  if (writen(fd, rep, sizeof(rep)) == -1) {
    perror("writen replyOK");
    return -1;
  }
  return 0;
}
static inline int discardData(int fd, char *buffer, size_t size) {
  int n, cnt=0;
  addToStats(PUT_DISCARDED, 1, size);
  do {
    if ((n=readn(fd, buffer, Min(size-cnt,BUFFER_SIZE))) <= 0) {
      if (n!=0) perror("read3");
      return -1;
    }
    cnt+=n;
  } while(cnt < size);
  return 0;
}
// it checks if the filename (i.e., lock file) is present, if it is, then
// it waits either until the file is removed or if the maxwait (ms) time expires
// returns 1 if the timeout expires
static inline int checkAndWaitIfExist(const char *filename, const long maxwait,
				      long *waittime, int *lockfilenotpresent) {
  const long wtime_ns = DEFAULT_TIMEOUT_MS*1000000;
  struct timespec ts={0, wtime_ns}; 

  DBG("GET keyLOCK=%s CHECK AND WAIT\n", filename);
  while(*lockfilenotpresent == 0) {
    if (access(filename, R_OK ) == 0) {   // lock file present, wait for it being removed
      nanosleep(&ts, NULL);
      *waittime = (*waittime) + (ts.tv_nsec/1000000);
      if (*waittime >= maxwait) {
	DBG("GET keyLOCK=%s TIMEOUT EXPIRED\n", filename);
	return 1;
      }
      ts.tv_sec  = 0;
      ts.tv_nsec = Min(wtime_ns, (maxwait - *waittime)* 1000000);
    } else *lockfilenotpresent = 1;
  }
  DBG("GET keyLOCK=%s NOT PRESENT or REMOVED\n", filename);
  return 0;
}
// atomically create the lock file, if it exists and ignoreifexist is false, then
// it waits a while (see checkAndWaitIfExist)
static inline int createLockAndWaitIfExist(const char *lockfile, char ignoreifexist,
					   const long maxtime, long *waittime,
					   int *lockfilenotpresent) {
  long maxwait = maxtime;
  int flock_fd;
  int timeout = 0;
  if ((flock_fd=open(lockfile, O_CREAT|O_WRONLY|O_EXCL, S_IRUSR|S_IWUSR))==-1) {
    if (errno == EEXIST) { // someone else has created the lock file before us
      *lockfilenotpresent = 0;
      if (!ignoreifexist) {
	if (waittime>0) { // we have already waited for some time, now wait for a bit less
	  *waittime=0;
	  maxwait /= 2;
	}
	timeout = checkAndWaitIfExist(lockfile, maxwait , waittime, lockfilenotpresent);
      }
      return timeout;
    }
    perror("creating lock file");
    return -1;
  }
  DBG("GET keyLOCK=%s LOCK FILE CREATED\n", lockfile);
  // lock file created 
  close(flock_fd);
  *lockfilenotpresent=1;
  return 0;
}


/* ------------------------------------------------------------------------- */
/* ------------------------------------------------------------------------- */

/* PUT_OP management
 * if file exits, the data file coming from the client is discarded
 * if file does not exist, write a temporary file and then rename it
 *
 * It always return OK but for internal errors. In case of internal error
 * the connection with the client is 'brutally' closed. 
 */
int handlePutOp(int fd, char *dir, hdr_t msg) {
  long size;
  int n;
  if ((n=readn(fd, &size, DATA_SIZE)) <= 0) {
    if (n<0) perror("read2");
    return -1;
  }
  size = convertToHost(size);
  if (size==0) return -1;  // connection closed
  
  char *filename = &msg[KEY];  // basename(&msg[KEY]);           
  char *filekey  = createPath(dir, filename, 0);     
  char *tmpfilename = NULL;
  char *buffer = NULL; 
  if ((buffer=malloc(BUFFER_SIZE)) == NULL) {
    perror("malloc");
    return -1;
  }

  DBG("PUT key=%s [fd=%d]\n", filename, fd);
  
  if (access(filekey, R_OK ) == 0 ) {
    DBG("PUT key=%s [fd=%d] FILE ALREADY EXISTS (OK)\n", filename, fd);
    free(filekey);
    if (discardData(fd, buffer, size)==-1) goto _puterror;
    if (replyOK(fd) == -1) goto _puterror;
  } else {
    free(filekey);
    int tmpfd = createTempFile(dir, filename, &tmpfilename);
    if (tmpfd == -1) {
      fprintf(stderr, "FATAL ERROR, cannot create tempory file for %s\n", filename);
      free(buffer);
      return -1;
    }
    
    char newfile[strlen(tmpfilename)+1];    
    
    FILE *fp = NULL;
    fp = fdopen(tmpfd, "w+");
    if (fp == NULL) {
      perror("fdopen");
      fprintf(stderr, "FATAL ERROR, cannot fdopen tempory file for %s\n", filename);
      goto _puterror;
    }
    int cnt=0;
    do {
      if ((n=readn(fd, buffer, Min(size-cnt,BUFFER_SIZE))) == -1) {
	perror("read3");
	goto _puterror;
      }
      if (n==0) goto _puterror;
      if (fwrite(buffer, 1, n, fp) != n) {
	perror("fwrite");
	goto _puterror;
      }
      cnt+=n;
    } while(cnt < size);
    
    fclose(fp);
    n = strlen(tmpfilename);
    strcpy(newfile, tmpfilename);
    newfile[n-strlen(".XXXXXX")]='\0';
    if (rename(tmpfilename, newfile) == -1) {
      perror("rename");
      fprintf(stderr, "FATAL ERROR, cannot rename %s --> %s\n", tmpfilename, newfile);
      goto _puterror;
    }
    DBG("PUT key=%s [fd=%d] OK\n", filename, fd);
    if (replyOK(fd) == -1) goto _puterror;

    addToStats(PUT_OK, 1, 0);

    // now I can remove ".flock" file, if exists
    strncat(newfile, ".flock", n+1);
    unlink(newfile);

    free(tmpfilename);
  }
  free(buffer);
  return 0;

 _puterror:
  addToStats(PUT_ERROR, 1, 0);
  free(buffer);
  free(tmpfilename);
  return -1;
}


/* GET_OP management:
 *
 *  Possible cases: 
 *   1. the file (key file) is present, cacheHIT
 *   2. the key file is not present
 *      2.1 the lock file is not present, create the lock file
 *          2.1.1 while creating the lock file, the key file appears then go to 2.2
 *          2.1.2 if the lock file is created successfully, then cacheMISS
 *      2.2 the lock file is present (we wait for at most maxtime)
 *          2.2.1 after some waiting time, the lock file disappear. Check whether the key file appears, if not create the lock file (ignoring if it already exists), then cacheMISS
 *          2.2.2 after some waiting time the lock file is still there (timeout expired). Check whether the original file appears, if not, then cacheMISS (presence of lock file ignored)
 *
 */
int handleGetOp(int fd, char *dir, long maxtime, hdr_t msg) {
  char *filename    = &msg[KEY];  // basename(&msg[KEY]);  
  char *tmpfilename = createPath(dir, filename, strlen(".flock")); 
  char  *mappedfile = NULL;
  FILE *fp;

  DBG("GET key=%s [fd=%d]\n", filename, fd);
  
  int  lockfilenotpresent = 0;  // flag
  int  timeout = 0;             // 1 if timeout expires
  long waittime = 0;

  while ((fp=fopen(tmpfilename, "r")) == NULL) {  // key file not present

    strcat(tmpfilename, ".flock");  // we have to create the lock file
    
    if (waittime>0 || lockfilenotpresent==1) {   // case 2.1. , 2.2.1. and 2.2.2. 

      int r=0;
      if ((r=createLockAndWaitIfExist(tmpfilename, (lockfilenotpresent == 0), 
				      maxtime, &waittime,
				      &lockfilenotpresent)) == -1) {
	if (cacheMiss(fd, "ERROR creating .flock file")<0) goto _geterror;
	addToStats(GET_ERROR, 1, 0);
	free(tmpfilename);
	return 0;
      }
      timeout+=r;
      if (lockfilenotpresent==1) {
	// let's try for the last time to check if the key file appeared
	tmpfilename[strlen(tmpfilename)-strlen(".flock")]='\0'; // remove ".flock" to retry
	if ((fp=fopen(tmpfilename, "r")) != NULL) break;
      }
      if (cacheMiss(fd, (timeout>0)?"TIMEOUT":NULL)<0) goto _geterror;
      DBG("GET key=%s [fd=%d] CACHE MISS\n", filename, fd);      
      if (timeout>0)
	addToStats(GET_TIMEOUT_MISS, 1, 0);
      else
	addToStats(GET_MISS, 1, 0);
      free(tmpfilename);
      return 0;
    }
    // it checks if the lock file exists, if yes it waits for a while
    timeout = checkAndWaitIfExist(tmpfilename, maxtime, &waittime, &lockfilenotpresent);
    tmpfilename[strlen(tmpfilename)-strlen(".flock")]='\0'; // remove ".flock" to retry
  }
  // file present (case 1.)

  // NOTE: the main file has been successfully opened, therefore the eventual presence
  //       of the lock file must be removed
  strcat(tmpfilename, ".flock");
  unlink(tmpfilename);
  
  if (fseek(fp, 0L, SEEK_END)==-1) {
    perror("fseek");
    cacheMiss(fd, "ERROR fseek");
    goto _geterror;
  }
  long size=ftell(fp);
  if (size<0) {
    perror("ftell");
    cacheMiss(fd, "ERROR ftell");
    goto _geterror;    
  }
  rewind(fp);

#if defined(CACHEMIRE_USE_MMAP)
  //
  // add support for explicit read and write to avoid mmap (compile time switch) 
  //
  mappedfile = mmap(NULL, size, PROT_READ, MAP_PRIVATE, fileno(fp), 0);
  if (mappedfile == MAP_FAILED) {
    perror("mmap");
    cacheMiss(fd, "ERROR mmap");
    goto _geterror;
  }  
  if (cacheHit(fd, mappedfile, size) == -1) goto _geterror;
#else
  if (cacheHitSendFile(fd, fp, size) == -1) goto _geterror;
#endif   
  DBG("GET key=%s [fd=%d] CACHE HIT\n", filename, fd);
  addToStats(GET_HIT, 1, 0);
  munmap(mappedfile, size);
  fclose(fp);   
  free(tmpfilename);
  return 0;

 _geterror:
  addToStats(GET_ERROR, 1, 0);
  if (mappedfile) munmap(mappedfile, size);
  fclose(fp);
  free(tmpfilename);
  return -1;
}

