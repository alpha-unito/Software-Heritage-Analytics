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
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <signal.h>
#include <sys/mman.h>
#include <util.h>
#include <conn.h>
#include <cachemire.h>
#include <threadpool.h>
#include <assert.h>


typedef struct {
  int argc;
  char **argv;
} partition_t;

void sendRequest(void *arg1, void *arg2);


int connect_to_addr(const struct sockaddr *addr, int addrlen) {
  int sockfd = socket(addr->sa_family, SOCK_STREAM, IPPROTO_TCP);
  if (sockfd == -1) {
    fprintf(stderr, "unable to create socket\n");
    return -1;
  }
  if (connect(sockfd, addr, addrlen) != 0) {
    close(sockfd);
    return -1;
  }
  return sockfd;
}

struct addrinfo *resolvehostname(const char* hostname, unsigned short port) {
    struct addrinfo hints;
    memset(&hints, '0', sizeof(hints));
    hints.ai_family   = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    hints.ai_flags    = AI_PASSIVE;   
    hints.ai_protocol  = 0;          
    hints.ai_canonname = NULL;
    hints.ai_addr      = NULL;
    hints.ai_next      = NULL;

    char service[6];
    memset(service, '0', sizeof(service));
    sprintf(service, "%hu", port);

    struct addrinfo *addrs = 0;
    int r = getaddrinfo(hostname, service, &hints, &addrs);
    if (r != 0) {
      fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(r));
      return NULL;
    }
    return addrs;
}


static inline void getop(char *arg, op_t *op, char **filename) {
  assert(arg[1] == ':');
  switch(arg[0]) {
  case 'G': {
    *op = GET_OP;
  } break;
  case 'P': {
    *op = PUT_OP;
  } break;
  default:
    abort();
  }
  *filename = &arg[2];
}


int main(int argc, char *argv[]) {
    if (argc <= 4) {
	fprintf(stderr, "\nusa: %s hostname port numthreads GP:file1 [GP:file2]\n\n", argv[0]);
	fprintf(stderr, "example: %s localhost 13000 2 P:file1.dat P:file2.dat G:file1.dat\n\n", argv[0]);
	fprintf(stderr, "NOTE: if numthreads>1 the G/P commands are PARTITIONED among them\n");
	
	exit(EXIT_FAILURE);
    }

    // ignore SIGPIPE
    struct sigaction s;
    memset(&s,0,sizeof(s));    
    s.sa_handler=SIG_IGN;
    if ( (sigaction(SIGPIPE,&s,NULL) ) == -1 ) {   
	perror("sigaction");
	return -1;
    } 
    
    char *hostname=argv[1];
    unsigned short port = atoi(argv[2]);
    int  nthreads=atoi(argv[3]);
    if (nthreads<=0) {
      fprintf(stderr, "ERROR: numthreads should be at lest 1\n");
      return -1;
    }
    struct addrinfo *addrs = NULL;
    addrs = resolvehostname(hostname, port);
    if (!addrs) {
      fprintf(stderr, "unable to resolve hostname=%s\n", hostname);
      return -1;
    }

    // trying to connect to check the address and the port
    int sockfd=-1;
    struct addrinfo *addr =NULL;
    for(struct addrinfo *_addr = addrs; _addr != 0; _addr = _addr->ai_next) {
      if ((sockfd=connect_to_addr(_addr->ai_addr, 
				  _addr->ai_addrlen)) == -1) {

	char host[256];
	getnameinfo(_addr->ai_addr, _addr->ai_addrlen, host, sizeof(host), NULL, 0,
		    NI_NUMERICHOST);
	fprintf(stderr, "unable to connect to %s:%d\n", host, port);
      } else {
	addr = _addr;
	break;
      }
    }

    if (sockfd==-1) {
      freeaddrinfo(addrs);
      return -1;
    }
    close(sockfd);
    assert(addr);
    
    threadpool_t *pool = createThreadPool(nthreads);
    if (!pool) {
      fprintf(stderr, "FATAL ERROR CREATING THREAD POOL\n");
      return -1;
    }
    long conn[nthreads];
    for(int i=0;i<nthreads;++i) {
      conn[i] = connect_to_addr(addr->ai_addr, addr->ai_addrlen);
      assert(conn[i]);
    }
    freeaddrinfo(addrs);

    int commands=(argc-4);
    int nparts = commands/nthreads;
    int r      = commands%nthreads;

    int iter= (nparts>=1)?nthreads:r;
    for(int i=0;i<iter;++i) {
      partition_t *p = malloc(sizeof(partition_t));
      assert(p);
      p->argc = nparts + ((r>i)?1:0);
      p->argv = &argv[4 + i*nparts + ((i<=r)?i:r)];

      //fprintf(stderr, "argc=%d, argv[0]=%s\n", p->argc, p->argv[0]);
      
      addToThreadPool(pool, sendRequest, (void*)conn[i], p);
    }
      
    destroyThreadPool(pool, 0);
    return 0;
}


void sendRequest(void *arg1, void *arg2) {
  int sockfd  = (long)arg1;
  int argc    = ((partition_t*)arg2)->argc;
  char **argv = ((partition_t*)arg2)->argv;
  free(arg2);
  
  for (int i=0;i<argc; ++i) {
    op_t op;
    char *filename=NULL;
    getop(argv[i], &op, &filename);
    if (strlen(filename) == 0) {
      fprintf(stderr, "Wrong filename=%s\n", filename);
      continue;
    }
    
    if (op == PUT_OP) {
      FILE *fp;
      if ((fp=fopen(filename, "r")) == NULL) {
	perror("fopen");
	fprintf(stderr, "Impossible to open filename=%s\n", filename);
	continue;
      }
      if (fseek(fp, 0L, SEEK_END)==-1) {
	perror("fseek");
	fprintf(stderr, "Impossible to get file size for filename=%s\n", filename);
	fclose(fp);
	continue;
      }
      long size=ftell(fp);
      if (size<0) {
	perror("ftell");
	fprintf(stderr, "Impossible to get file size for filename=%s\n", filename);
	fclose(fp);
	continue;
      }
      rewind(fp);
      
      char  *mappedfile = NULL;
      mappedfile = mmap(NULL, size, PROT_READ, MAP_PRIVATE, fileno(fp), 0);
      if (mappedfile == MAP_FAILED) {
	perror("mmap");
	fprintf(stderr, "Impossible to memory map filename=%s\n", filename);
	fclose(fp);
	continue;
      }
      fclose(fp);
      
      fprintf(stderr, "PUT_OP, file %s opened, size=%ld\n", filename, size);
      
      put_hdr_t hdr;
      setPutOp(hdr, filename, size);
      
      if (writen(sockfd, hdr, sizeof(hdr))<0) abort();
      if (writen(sockfd, mappedfile, size)<0) abort();
      munmap(mappedfile, size);
      
      rpl_hdr_t rep;
      if (readn(sockfd, rep, sizeof(rep))<0) abort();
      if (rep[OP] != OP_OK) {
	if (rep[REP_SIZE] != 0) abort();
      }
      fprintf(stderr, "OP_OK\n");
      
    } else {
      get_hdr_t hdr;
      setGetOp(hdr, filename);
      
      if (writen(sockfd, &hdr, sizeof(hdr))<0) abort();
      rpl_hdr_t rep;
      if (readn(sockfd, rep, sizeof(rep))<0) abort();
      if (rep[OP] == OP_OK) {
	size_t size = getSize(rep);
	if (size==0) abort();
	
	// leggo tutto il file in memoria
	char *buffer = malloc(size);
	if (readn(sockfd, buffer, size) <0) abort();
	free(buffer);
	
	fprintf(stderr, "GET OK size=%ld\n", size);
      }
      if (rep[OP] == OP_FAILED) {
	size_t size;
	if ((size=getSize(rep)) != 0) {
	  char *buffer = malloc(size);
	  if (readn(sockfd, buffer, size) <0) abort();	    
	  fprintf(stderr, "file %s NOT IN CACHE [%s]\n", filename, buffer);
	  free(buffer);
	} else
	  fprintf(stderr, "file %s NOT IN CACHE\n", filename);
      }
    }
  }
}
