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
#if !defined(_STATS_H)
#define _STATS_H

enum {SERVER_BUSY=1,
      PUT_DISCARDED,
      PUT_OK,
      PUT_ERROR,
      GET_HIT,
      GET_MISS,
      GET_TIMEOUT_MISS,
      GET_ERROR};

void printStats();
void addToStats(int tag, long value1, long value2);


#endif /* _STATS_H */
