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

#ifndef CACHEMIRE_H_
#define CACHEMIRE_H_

#include <endian.h>
#include <conn.h>


// size of header's components
#define OP_SIZE         1
#define DATA_SIZE       8

#if !defined(CACHEMIRE_KEY_SIZE)
#define KEY_SIZE       40
#else
#define KEY_SIZE       CACHEMIRE_KEY_SIZE
#endif

#define HDR_SIZE      (OP_SIZE+KEY_SIZE)
#define PUT_HDR_SIZE  (HDR_SIZE + DATA_SIZE)
#define GET_HDR_SIZE  (HDR_SIZE)
#define RPL_HDR_SIZE  (OP_SIZE+DATA_SIZE)

// some default configuration values
#define DEFAULT_SRV_PORT         13000   // option -p
#define DEFAULT_N_THREADS            8   // option -n
#define DEFAULT_MAX_NTHREADS        64   // no option
#define DEFAULT_TIMEOUT_MS          30   // 30ms -- no option
#define DEFAULT_MAX_WAITTIME_MS   5000   //  5s -- option -t
// max amount of data that is read from the client socket with a
// single call
#define BUFFER_SIZE              16384   // no option

// entries to extract the data from the header
#define OP         0
#define KEY        1
#define SIZE       (1+KEY_SIZE)
#define REP_SIZE   1


// operations
typedef enum {
	      /* ------------------------------------------ */
	      /*    from client to server                   */
	      /* ------------------------------------------ */	      
	      GET_OP      = 0,   
	      PUT_OP      = 1,

	      /* ------------------------------------------ */
	      /*    from server to client                   */
	      /* ------------------------------------------ */
	      OP_OK       = 20,  
	      OP_FAILED   = 21
} op_t;

// message types 
typedef char hdr_t[HDR_SIZE];               // generic header
typedef char put_hdr_t[PUT_HDR_SIZE];       // put message
typedef char get_hdr_t[GET_HDR_SIZE];       // get message
typedef char rpl_hdr_t[RPL_HDR_SIZE];       // reply message

// this is the type of the shared state used by each thread in the pool
typedef struct {
  int   pipe_back;
  long  maxwaitget;
  char *cachedir;
} serverState_t;

/* ------------------------------------------------------------ */
/* -------------- some utility functions ---------------------- */
/* ------------------------------------------------------------ */
static inline size_t convertToNetwork(size_t s) { return htobe64(s); }
static inline size_t convertToHost(size_t s)    { return be64toh(s); }
static inline void setPutOp(put_hdr_t hdr, char *key, long size) {
  hdr[OP] = PUT_OP;
  memcpy(&hdr[KEY], key, KEY_SIZE);
  long *sz = (long*)(&hdr[HDR_SIZE]);
  *sz = convertToNetwork(size);
}
static inline void setGetOp(get_hdr_t hdr, char *key) {
  hdr[OP] = GET_OP;
  memcpy(&hdr[KEY], key, KEY_SIZE);
}
static inline void setOK(rpl_hdr_t hdr) {
  hdr[OP]   = OP_OK;
  long *sz = (long*)(&hdr[REP_SIZE]);
  *sz      = 0;
}
static inline void setSize(rpl_hdr_t hdr, size_t size) {
  long *sz = (long*)(&hdr[REP_SIZE]);
  *sz        = convertToNetwork(size);
}
static inline void setFAIL(rpl_hdr_t hdr, const char *str) {
  hdr[OP]   = OP_FAILED;
  long *sz = (long*)(&hdr[REP_SIZE]);
  *sz      = 0;
  if (str) setSize(hdr, strlen(str) + 1);
}  
static inline size_t getSize(rpl_hdr_t hdr) {
  long *sz = (long*)(&hdr[REP_SIZE]);
  return convertToHost(*sz);
}
  

#endif /* CACHEMIRE_H_ */
