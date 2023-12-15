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

/**
 * @file threadpool.c
 * @brief Implementation of the Threadpool API
 */

#include <stdio.h>
#include <assert.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <errno.h>
#include <util.h>
#include <threadpool.h>
#include <stats.h>

/**
 * @function void *threadpool_thread(void *threadpool)
 * @brief proxy function of the genering worker in the pool
 */
static void *workerpool_thread(void *threadpool) {    
    threadpool_t *pool = (threadpool_t *)threadpool; // cast
    taskfun_t task;  // generic task
    pthread_t self = pthread_self();
    int myid = -1;

    // not particularly efficient, but doesn't matter here ....
    do {
	for (int i=0;i<pool->numthreads;++i)
	    if (pthread_equal(pool->threads[i], self)) {
		myid = i;
		break;
	    }
    } while (myid < 0 && !pool->exiting);

    LOCK_RETURN(&(pool->lock), NULL);
    for (;;) {

        // waiting a task
        while((pool->count == 0) && (!pool->exiting)) {
            pthread_cond_wait(&(pool->cond), &(pool->lock));
	}

        if (pool->exiting > 1) break; // forced exit
	// if there are pending tasks, execute them before terminating
	if (pool->exiting == 1 && !pool->count) break;  

        task.fun      = pool->queue[pool->head].fun;
        task.arg1     = pool->queue[pool->head].arg1;
	task.arg2     = pool->queue[pool->head].arg2;

        pool->head++; pool->count--;
        pool->head = (pool->head == pool->queue_size) ? 0 : pool->head;
	
        UNLOCK_RETURN(&(pool->lock), NULL);

        // running the task
        (*(task.fun))(task.arg1, task.arg2);
	
	LOCK_RETURN(&(pool->lock), NULL);
    }
    UNLOCK_RETURN(&(pool->lock), NULL);
    return NULL;
}


static int freePoolResources(threadpool_t *pool) {
    if(pool->threads) {
        free(pool->threads);
        free(pool->queue);
	
        pthread_mutex_destroy(&(pool->lock));
        pthread_cond_destroy(&(pool->cond));
    }
    free(pool);    
    return 0;
}

threadpool_t *createThreadPool(int numthreads) {
  if(numthreads <= 0) {
    errno = EINVAL;
    return NULL;
  }
    
  threadpool_t *pool = (threadpool_t *)malloc(sizeof(threadpool_t));
  if (pool == NULL) return NULL;
  
  pool->numthreads   = 0;
  pool->queue_size   = numthreads;
  pool->head         = pool->tail = pool->count = 0;
  pool->exiting      = 0;
  
  pool->threads = (pthread_t *)malloc(sizeof(pthread_t) * numthreads);
  if (pool->threads == NULL) {
    free(pool);
    return NULL;
  }
  pool->queue = (taskfun_t *)malloc(sizeof(taskfun_t) * numthreads);
  if (pool->queue == NULL) {
    free(pool->threads);
    free(pool);
    return NULL;
  }
  if ((pthread_mutex_init(&(pool->lock), NULL) != 0) ||
      (pthread_cond_init(&(pool->cond), NULL) != 0))  {
    free(pool->threads);
    free(pool->queue);
    free(pool);
    return NULL;
  }
  for(int i = 0; i < numthreads; i++) {
    if(pthread_create(&(pool->threads[i]), NULL,
		      workerpool_thread, (void*)pool) != 0) {
      // FATAL error....
      destroyThreadPool(pool, 1);
      errno = EFAULT;
      return NULL;
    }
    pool->numthreads++;
  }
  return pool;
}


int destroyThreadPool(threadpool_t *pool, int force) {    
    if(pool == NULL || force < 0) {
	errno = EINVAL;
	return -1;
    }

    LOCK_RETURN(&(pool->lock), -1);

    pool->exiting = 1 + force;

    if (pthread_cond_broadcast(&(pool->cond)) != 0) {
      UNLOCK_RETURN(&(pool->lock),-1);
      errno = EFAULT;
      return -1;
    }
    UNLOCK_RETURN(&(pool->lock), -1);

    for(int i = 0; i < pool->numthreads; i++) {
	if (pthread_join(pool->threads[i], NULL) != 0) {
	    errno = EFAULT;
	    UNLOCK_RETURN(&(pool->lock),-1);
	    return -1;
	}
    }
    freePoolResources(pool);
    return 0;
}

int addToThreadPool(threadpool_t *pool, void (*f)(void *,void*), void *arg1, void* arg2) {
    if(pool == NULL || f == NULL) {
	errno = EINVAL;
	return -1;
    }

    LOCK_RETURN(&(pool->lock), -1);
    // either the queue is full or we are in the termination phase
    if (pool->count >= pool->queue_size || pool->exiting)  {
      UNLOCK_RETURN(&(pool->lock),-1);
      return 1; 
    }
    
    pool->queue[pool->tail].fun = f;
    pool->queue[pool->tail].arg1 = arg1;
    pool->queue[pool->tail].arg2 = arg2;
    pool->count++;    
    pool->tail++;
    if (pool->tail >= pool->queue_size) pool->tail = 0;
    
    int r;
    if((r=pthread_cond_signal(&(pool->cond))) != 0) {
      UNLOCK_RETURN(&(pool->lock),-1);
      errno = r;
      return -1;
    }

    UNLOCK_RETURN(&(pool->lock),-1);
    return 0;
}


/**
 * @function void *thread_proxy(void *argl)
 * @brief funzione eseguita dal thread worker che non appartiene al pool
 */
static void *proxy_thread(void *arg) {    
    taskfun_t *task = (taskfun_t*)arg;
    // running the task
    (*(task->fun))(task->arg1, task->arg2);
    
    free(task);
#if !defined(NO_STATISTICS)    
    addToStats(SERVER_BUSY, -1, 0);
#endif    
    return NULL;
}

// it spawns a detached thread that executes f(arg1, arg2)
int spawnThread(void (*f)(void*,void*), void* arg1, void* arg2) {
    if (f == NULL) {
	errno = EINVAL;
	return -1;
    }

    taskfun_t *task = malloc(sizeof(taskfun_t));   // la memoria verra' liberata dal proxy 
    if (!task) return -1;
    task->fun = f;
    task->arg1 = arg1;
    task->arg2 = arg2;

    pthread_t thread;
    pthread_attr_t attr;
    if (pthread_attr_init(&attr) != 0) return -1;
    if (pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED) != 0) return -1;
    if (pthread_create(&thread, &attr,
		      proxy_thread, (void*)task) != 0) {
	free(task);
	errno = EFAULT;
	return -1;
    }
#if !defined(NO_STATISTICS)    
    addToStats(SERVER_BUSY, 1, 0);
#endif    
    return 0;
}
