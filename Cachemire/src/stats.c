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

#include <stdio.h>
#include <stdatomic.h>
#include <pthread.h>
#include <stats.h>

static atomic_long puts_ok               = 0;
static atomic_long puts_error            = 0;
static atomic_long puts_discarded        = 0;
static atomic_long puts_discarded_bytes  = 0;
static atomic_long gets_hit              = 0;
static atomic_long gets_miss             = 0;
static atomic_long gets_timeo_miss       = 0;
static atomic_long gets_error            = 0;
static atomic_long tot_extra_threads     = 0;
static atomic_long cur_extra_threads     = 0;
static atomic_long max_extra_threads     = 0;


pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

void addToStats(int tag, long value1, long value2) {
  switch(tag) {
  case SERVER_BUSY: {
    pthread_mutex_lock(&mutex);
    if (value1>0)
      atomic_fetch_add_explicit(&tot_extra_threads, value1, memory_order_relaxed);
    long cet = atomic_fetch_add_explicit(&cur_extra_threads, value1, memory_order_relaxed);
    long max = atomic_load_explicit(&max_extra_threads, memory_order_relaxed);
    if (cet > max)
      atomic_store_explicit(&max_extra_threads, cet, memory_order_relaxed);
    pthread_mutex_unlock(&mutex);
  } break;
  case PUT_DISCARDED: {
    atomic_fetch_add_explicit(&puts_discarded,  value1, memory_order_relaxed);
    atomic_fetch_add_explicit(&puts_discarded_bytes,value2, memory_order_relaxed);
  } break;
  case PUT_OK: {
    atomic_fetch_add_explicit(&puts_ok,  value1, memory_order_relaxed);
  } break;
  case PUT_ERROR: {
    atomic_fetch_add_explicit(&puts_error,  value1, memory_order_relaxed);
  } break;
  case GET_HIT: {
    atomic_fetch_add_explicit(&gets_hit,  value1, memory_order_relaxed);
  } break;
  case GET_MISS: {
    atomic_fetch_add_explicit(&gets_miss,  value1, memory_order_relaxed);
  } break;
  case GET_TIMEOUT_MISS: {
    atomic_fetch_add_explicit(&gets_timeo_miss,  value1, memory_order_relaxed);
  } break;
  case GET_ERROR: {
    atomic_fetch_add_explicit(&gets_error,  value1, memory_order_relaxed);
  } break;
  default:;
  }
}


void printStats() {
  fprintf(stdout,
	  "\n -- cachemire stats -----------------\n");
  fprintf(stdout,
	  " n. gets hit          :  %12ld\n", atomic_load_explicit(&gets_hit, memory_order_relaxed));
  fprintf(stdout,
	  " n. gets miss         :  %12ld\n", atomic_load_explicit(&gets_miss, memory_order_relaxed));
  fprintf(stdout,
	  " n. gets timeout miss :  %12ld\n", atomic_load_explicit(&gets_timeo_miss, memory_order_relaxed));
  fprintf(stdout,
	  " n. gets error        :  %12ld\n", atomic_load_explicit(&gets_error, memory_order_relaxed));
  fprintf(stdout,
	  " n. puts ok           :  %12ld\n", atomic_load_explicit(&puts_ok, memory_order_relaxed));
  fprintf(stdout,
	  " n. puts discarded    :  %12ld\n", atomic_load_explicit(&puts_discarded, memory_order_relaxed));
  fprintf(stdout,
	  "       |--->  KBytes  :  %12ld\n", atomic_load_explicit(&puts_discarded_bytes, memory_order_relaxed) / 1024);
  fprintf(stdout,
	  " n. puts error        :  %12ld\n", atomic_load_explicit(&puts_error, memory_order_relaxed));
  fprintf(stdout,
	  " tot ext. threads     :  %12ld\n", atomic_load_explicit(&tot_extra_threads, memory_order_relaxed));
  fprintf(stdout,
	  " cur ext. threads     :  %12ld\n", atomic_load_explicit(&cur_extra_threads, memory_order_relaxed));
  fprintf(stdout,
	  " max ext. threads     :  %12ld\n", atomic_load_explicit(&max_extra_threads, memory_order_relaxed));
  
  fprintf(stdout,
	  " ------------------------------------\n");
  fflush(stdout);
}
