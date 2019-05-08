/* Cydia Substrate - Powerful Code Insertion Platform
 * Copyright (C) 2008-2011  Jay Freeman (saurik)
*/

/* GNU Lesser General Public License, Version 3 {{{ */
/*
 * Substrate is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Substrate is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Substrate.  If not, see <http://www.gnu.org/licenses/>.
**/
/* }}} */

#ifndef SUBSTRATE_ARM_HPP
#define SUBSTRATE_ARM_HPP

enum A$r {
    A$r0, A$r1, A$r2, A$r3,
    A$r4, A$r5, A$r6, A$r7,
    A$r8, A$r9, A$r10, A$r11,
    A$r12, A$r13, A$r14, A$r15,
    A$sp = A$r13,
    A$lr = A$r14,
    A$pc = A$r15
};

enum A$c {
    A$eq, A$ne, A$cs, A$cc,
    A$mi, A$pl, A$vs, A$vc,
    A$hi, A$ls, A$ge, A$lt,
    A$gt, A$le, A$al,
    A$hs = A$cs,
    A$lo = A$cc
};

template<class T> static T xabs(T _Val);

#define A$mrs_rm_cpsr(rd) /* mrs rd, cpsr */ \
    (0xe10f0000 | ((rd) << 12))
#define A$msr_cpsr_f_rm(rm) /* msr cpsr_f, rm */ \
    (0xe128f000 | (rm))
#define A$ldr_rd_$rn_im$(rd, rn, im) /* ldr rd, [rn, #im] */ \
    (0xe5100000 | ((im) < 0 ? 0 : 1 << 23) | ((rn) << 16) | ((rd) << 12) | xabs(im))
#define A$str_rd_$rn_im$(rd, rn, im) /* sr rd, [rn, #im] */ \
    (0xe5000000 | ((im) < 0 ? 0 : 1 << 23) | ((rn) << 16) | ((rd) << 12) | xabs(im))
#define A$sub_rd_rn_$im(rd, rn, im) /* sub, rd, rn, #im */ \
    (0xe2400000 | ((rn) << 16) | ((rd) << 12) | (im & 0xff))
#define A$blx_rm(rm) /* blx rm */ \
    (0xe12fff30 | (rm))
#define A$mov_rd_rm(rd, rm) /* mov rd, rm */ \
    (0xe1a00000 | ((rd) << 12) | (rm))
#define A$ldmia_sp$_$rs$(rs) /* ldmia sp!, {rs} */ \
    (0xe8b00000 | (A$sp << 16) | (rs))
#define A$stmdb_sp$_$rs$(rs) /* stmdb sp!, {rs} */ \
    (0xe9200000 | (A$sp << 16) | (rs))
#define A$stmia_sp$_$r0$  0xe8ad0001 /* stmia sp!, {r0}   */
#define A$bx_r0           0xe12fff10 /* bx r0             */

#endif//SUBSTRATE_ARM_HPP
