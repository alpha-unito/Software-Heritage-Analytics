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
#include <sys/select.h>
#include <sys/mman.h>

#include <util.h>
#include <conn.h>
#include <cachemire.h>


extern int handleGetOp(int fd, char *dir, long maxwaitget, hdr_t msg);
extern int handlePutOp(int fd, char *dir, hdr_t msg);

// worker thread function
// it handles one request 
void threadF(void *arg1, void *arg2) {
  assert(arg1 && arg2);
  long fd       = (long)arg1;
  serverState_t *state = (serverState_t*)arg2;
  int pipe_back        = state->pipe_back;
  long maxwaitget      = state->maxwaitget;
  char *cachedir       = state->cachedir;
  
  char msg[HDR_SIZE+1];   // NOTE: this is a non-generic hdr_t, one extra byte is needed for storing '\0' since the key part is managed as a string
  int n;	
  if ((n=readn(fd, &msg, HDR_SIZE)) == -1) {
    perror("read1");
    goto _closeconn;
  }
  DBG("THREAD(%ld): read msg [fd=%d] (n=%d)\n", pthread_self(), fd, n);
  
  if (n==0) goto _closeconn;
  
  switch(msg[OP]) {
  case GET_OP:
    if (handleGetOp(fd, cachedir, maxwaitget, msg) == -1) goto _closeconn;
    break;
  case PUT_OP: 
    if (handlePutOp(fd, cachedir, msg) == -1) goto _closeconn;
    break;
  default: {
    fprintf(stderr, "ERROR, invalid op '%c'\n", msg[OP]);
    goto _closeconn;
  }
  }
  
  if (write(pipe_back, &fd, sizeof(int)) == -1) {
    perror("write pipe_back");
    fprintf(stderr, "THE IMPOSSIBLE HAS HAPPENED, EXIT!\n");    
    abort();
  }
  return;
 _closeconn:
  fd = -fd;
  if (write(pipe_back, &fd, sizeof(int))==-1) {
    perror("write pipe_back");
    fprintf(stderr, "THE IMPOSSIBLE HAS HAPPENED, EXIT!\n");
    abort();
  }
}
