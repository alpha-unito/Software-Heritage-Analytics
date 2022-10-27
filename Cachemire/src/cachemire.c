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
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include <ctype.h>
#include <signal.h>
#include <sys/select.h>

#include <cachemire.h>
#include <threadpool.h>
#include <conn.h>
#include <util.h>
#include <threadF.h>
#include <stats.h>

/**
 *  @struct sigHandlerArgs_t
 *  @brief struttura contenente le informazioni da passare al signal handler thread
 *
 */
typedef struct {
    sigset_t     *set;           /// masked signals
    int           signal_pipe;   /// pipe used to notify the listener thread
} sigHandler_t;

// ------------ globals ---------------------
static long  server_port = DEFAULT_SRV_PORT;
static long  nthreads    = DEFAULT_N_THREADS;
static long  maxwaitget  = DEFAULT_MAX_WAITTIME_MS;
static char *cachedir    = NULL;
// ------------------------------------------

// signal handler thread function
static void *sigHandler(void *arg) {
    sigset_t *set = ((sigHandler_t*)arg)->set;
    int fd_pipe   = ((sigHandler_t*)arg)->signal_pipe;

    for( ;; ) {
	int sig;
	int r = sigwait(set, &sig);
	if (r != 0) {
	    perror("FATAL ERROR 'sigwait'");
	    return NULL;
	}

	switch(sig) {
	case SIGHUP:
	case SIGINT:
	case SIGTERM:
	case SIGQUIT:{
	    close(fd_pipe);  // notify the listener thread 
	    return NULL;
	}
	case SIGUSR1: {
	  printStats();
	} break;
	default:  ; 
	}
    }
    return NULL;	   
}

static inline int updatemax(fd_set set, int fdmax) {
  for(int i=(fdmax-1);i>=0;--i)
    if (FD_ISSET(i, &set)) return i;
  assert(1==0);
  return fdmax;
}

static void usage(const char *argv0) {
    printf("\n--------------------\n");
    printf("Usage: %s -d cachedir [options]\n",argv0);
    printf("\nOptions:\n");
    printf(" -n set the n. of threads in pool        (default %ld)\n", nthreads);
    printf(" -p listen port                          (default %ld)\n", server_port);
    printf(" -t max wait time (in ms) for the GET_OP (default %ld)\n", maxwaitget);
    printf("--------------------\n");
}

static int parseArgs(int argc, char *argv[]) {
    extern char *optarg;
    const char optstr[]="d:n:p:t:";
    
    long opt;
    while((opt = getopt(argc, argv, optstr)) != -1) {
      switch(opt) {
      case 'd': {
	cachedir = strdup(optarg);
      } break;
      case 'n': {
	if (isNumber(optarg, &nthreads)!=0) {
	  fprintf(stderr, "Error: wrong '-n' option\n");
	  usage(argv[0]);
	  return -1;
	}
      } break;
      case 't': {
	if (isNumber(optarg, &maxwaitget)!=0) {
	  fprintf(stderr, "Error: wrong '-t' option\n");
	  usage(argv[0]);
	  return -1;
	}
      } break;
      case 'p': {
	if (isNumber(optarg, &server_port)!=0) {
	  fprintf(stderr, "Error: wrong '-t' option\n");
	  usage(argv[0]);
	  return -1;
	}
      } break;
      default:
	usage(argv[0]);
	return -1;
      }
    }
    if (cachedir==NULL) {
      fprintf(stderr, "Error: -d option missing or invalid value\n");
      usage(argv[0]);
      return -1;
    }
    return 0;
}


    
static void checkargs(int argc, char* argv[]) {
    if (argc == 1) {
	usage(argv[0]);
	_exit(EXIT_FAILURE);
    }
    if (parseArgs(argc,argv) == -1)
      _exit(EXIT_FAILURE);
    
    // checking if the directory exists
    struct stat statbuf;
    if (stat(cachedir, &statbuf) == -1) {
      perror("stat");
      fprintf(stderr, "FATAL ERROR, cannot stat the directory %s, the directory must exist\n", cachedir);
      _exit(EXIT_FAILURE);
    }
    if(!S_ISDIR(statbuf.st_mode)) {
	fprintf(stderr, "%s is not a directory\n", cachedir);
	_exit(EXIT_FAILURE);
    }    
    if (nthreads<=0 || nthreads>DEFAULT_MAX_NTHREADS) {
      fprintf(stderr, "the number of threads in the pool must be greater than zero and less than %d)\n\n", DEFAULT_MAX_NTHREADS+1);
      usage(argv[0]);
      _exit(EXIT_FAILURE);
    }
        
}
int main(int argc, char *argv[]) {
    checkargs(argc, argv);	    

    if (cachedir[strlen(cachedir)-1]=='/')
      cachedir[strlen(cachedir)-1]='\0';

    int listenfd=-1;
    sigset_t mask;
    sigemptyset(&mask);
    sigaddset(&mask, SIGHUP);
    sigaddset(&mask, SIGINT); 
    sigaddset(&mask, SIGQUIT);
    sigaddset(&mask, SIGTERM);
    sigaddset(&mask, SIGUSR1);
    
    if (pthread_sigmask(SIG_BLOCK, &mask,NULL) != 0) {
	fprintf(stderr, "FATAL ERROR\n");
	goto _exit;
    }

    // ignore SIGPIPE
    struct sigaction s;
    memset(&s,0,sizeof(s));    
    s.sa_handler=SIG_IGN;
    if ( (sigaction(SIGPIPE,&s,NULL) ) == -1 ) {   
	perror("sigaction");
	goto _exit;
    } 

    // pipe used between signal handler thread and the listener thread to notify a signal
    int signal_pipe[2];
    if (pipe(signal_pipe)==-1) {
	perror("pipe");
	goto _exit;
    }
    pthread_t sighandler_thread;
    sigHandler_t handlerArgs = { &mask, signal_pipe[1] };
   
    if (pthread_create(&sighandler_thread, NULL, sigHandler, &handlerArgs) != 0) {
	fprintf(stderr, "ERROR: creating the signal handler thread\n");
	goto _exit;
    }

    // pipe used between the threads in the pool and the listener thread to notify op completion
    int back_pipe[2];
    if (pipe(back_pipe)==-1) {
	perror("pipe");
	goto _exit;
    }

    serverState_t shared_state = { back_pipe[1], maxwaitget, cachedir };
    threadpool_t *pool = createThreadPool(nthreads);
    if (!pool) {
	fprintf(stderr, "FATAL ERROR CREATING THREAD POOL\n");
	goto _exit;
    }
    
    if ((listenfd= socket(AF_INET, SOCK_STREAM, 0)) == -1) {
	perror("socket");
	goto _exit;
    }
  
    if (setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR, &(int){1}, sizeof(int)) < 0)
      perror("setsockopt(SO_REUSEADDR) failed");
    
#ifdef SO_REUSEPORT
    if (setsockopt(listenfd, SOL_SOCKET, SO_REUSEPORT, &(int){1}, sizeof(int)) < 0) 
      perror("setsockopt(SO_REUSEPORT) failed");
#endif
    
    
    struct sockaddr_in serv_addr;
    memset(&serv_addr, '0', sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(server_port);

    if (bind(listenfd, (struct sockaddr*)&serv_addr,sizeof(serv_addr)) == -1) {
	perror("bind");
	goto _exit;
    }
    if (listen(listenfd, MAXBACKLOG) == -1) {
	perror("listen");
	goto _exit;
    }

    // set standard output unbuffered
    setbuf(stdout, NULL);
    
    fd_set set, tmpset;
    FD_ZERO(&set);
    FD_ZERO(&tmpset);

    FD_SET(listenfd, &set);        
    FD_SET(signal_pipe[0], &set);  
    int fdmax = (listenfd > signal_pipe[0]) ? listenfd : signal_pipe[0];
    FD_SET(back_pipe[0], &set);
    if (fdmax<back_pipe[0]) fdmax = back_pipe[0];
    
    long mustterminate=0;
    while(!mustterminate) {
      tmpset = set;
      if (select(fdmax+1, &tmpset, NULL, NULL, NULL) == -1) {
	if (errno==EINTR) continue;
	perror("select");
	goto _exit;
      }
      for(long idx=0; idx <= fdmax; idx++) {
	if (FD_ISSET(idx, &tmpset)) {
	  if (idx==listenfd) {
	    int connfd;
	    if ((connfd = accept(listenfd, (struct sockaddr*)NULL ,NULL)) == -1) {
	      perror("accept");
	      goto _exit;
	    }
	    FD_SET(connfd, &set);
	    if (connfd>fdmax) fdmax = connfd;	      
	  } else {
	    if (idx==signal_pipe[0]) {
	      mustterminate = 1; // termination protocol started
	    } else {
	      if (idx==back_pipe[0]) {
		int fd;
		if (read(back_pipe[0], &fd, sizeof(int))==-1) {
		  fprintf(stderr, "FATAL ERROR, reading from back_pipe\n");
		  goto _exit;
		}
		if (fd<0) {
		  fd=-fd;
		  if (fd == fdmax) fdmax=updatemax(set, fd);
		  close(fd);
		} else {
		  FD_SET(fd, &set);
		  if (fd>fdmax) fdmax = fd;
		}
	      } else {

		
		FD_CLR(idx, &set); 

		int r =addToThreadPool(pool, threadF, (void*)idx, (void*)&shared_state);
		if (r<0) { // error
		  fprintf(stderr, "FATAL ERROR, adding task to the thread pool\n");
		  close(idx);
		} else {
		  if (r!=0) {
		    // we have to spawn a worker thread
		    DBG("SERVER TOO BUSY, SPAWNING ONE EXTRA THREAD\n");
		    if (spawnThread(threadF, (void*)idx, (void*)&shared_state) < 0) {
		      fprintf(stderr, "FATAL ERROR, spawning new thread\n");
		      close(idx);
		    } 
		  }
		}
	      }
	    }
	  }
	}
      } // for
    }
    
    destroyThreadPool(pool, 0);  

    // waiting signal handler thread
    pthread_join(sighandler_thread, NULL);
    close(listenfd);
    free(cachedir);
    printStats();
    return 0;    
 _exit:
    close(listenfd);
    free(cachedir);
    return -1;
}

