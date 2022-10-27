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

#ifndef THREADPOOL_H_
#define THREADPOOL_H_

#include <pthread.h>


 
/**
 *  @struct threafun_t
 *  @brief generic task executed by a thread in the pool
 *
 */
typedef struct taskfun_t {
  void (*fun)(void *, void *);
  void *arg1;
  void *arg2;
} taskfun_t;

/**
 *  @struct threadpool
 *  @brief Pool monitor object
 */
typedef struct threadpool_t {
    pthread_mutex_t  lock;    
    pthread_cond_t   cond;    
    pthread_t      * threads; // workers' ids
    int numthreads;           // 
    taskfun_t *queue;         // internal queue
    int queue_size;           // 
    int head, tail;           // queue's pointers 
    int count;                // tasks in the queue
    int exiting;              // if > 0 the termination protocol has started, if 1 the thread waits for current job completionil thread aspetta che non ci siano piu' lavori in coda
} threadpool_t;

/**
 * @function createThreadPool
 * @brief Create the thread pool object.
 */
threadpool_t *createThreadPool(int numthreads);

/**
 * @function destroyThreadPool
 * @brief Destroy the pool.
 */
int destroyThreadPool(threadpool_t *pool, int force);

/**
 * @function addTaskToThreadPool
 * @brief It adds a task to the pool, if there are available threads the task is assigned, otherwise it returns -1.
 */
int addToThreadPool(threadpool_t *pool, void (*fun)(void *,void*),void *arg1, void *arg2);


/**
 * @function spawnThread
 * @brief It spawn a detached thread to execute the function passed as parameter. 
 */
int spawnThread(void (*f)(void*,void*), void* arg1, void *arg2);

#endif /* THREADPOOL_H_ */

