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

#define SubstrateInternal
#include "CydiaSubstrate.h"

#include <sys/mman.h>

#define _trace() do { \
    MSLog(MSLogLevelNotice, "_trace(%u)", __LINE__); \
} while (false)

#if defined(__i386__) || defined(__x86_64__)
#include "hde64.h"
#endif

#include "SubstrateDebug.hpp"

#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <cmath>

#ifdef __arm__
/* WebCore (ARM) PC-Relative:
X    1  ldr r*,[pc,r*] !=
     2 fldd d*,[pc,#*]
X    5  str r*,[pc,r*] !=
     8 flds s*,[pc,#*]
   400  ldr r*,[pc,r*] ==
   515  add r*, pc,r*  ==
X 4790  ldr r*,[pc,#*]    */

// x=0; while IFS= read -r line; do if [[ ${#line} -ne 0 && $line == +([^\;]): ]]; then x=2; elif [[ $line == ' +'* && $x -ne 0 ]]; then ((--x)); echo "$x${line}"; fi; done <WebCore.asm >WebCore.pc
// grep pc WebCore.pc | cut -c 40- | sed -Ee 's/^ldr *(ip|r[0-9]*),\[pc,\#0x[0-9a-f]*\].*/ ldr r*,[pc,#*]/;s/^add *r[0-9]*,pc,r[0-9]*.*/ add r*, pc,r*/;s/^(st|ld)r *r([0-9]*),\[pc,r([0-9]*)\].*/ \1r r\2,[pc,r\3]/;s/^fld(s|d) *(s|d)[0-9]*,\[pc,#0x[0-9a-f]*].*/fld\1 \2*,[pc,#*]/' | sort | uniq -c | sort -n

#include "SubstrateARM.hpp"

#define T$Label(l, r) \
    (((r) - (l)) * 2 - 4 + ((l) % 2 == 0 ? 0 : 2))

#define T$pop_$r0$ 0xbc01 // pop {r0}
#define T$b(im) /* b im */ \
    (0xde00 | (im & 0xff))
#define T$blx(rm) /* blx rm */ \
    (0x4780 | (rm << 3))
#define T$bx(rm) /* bx rm */ \
    (0x4700 | (rm << 3))
#define T$nop /* nop */ \
    (0x46c0)

#define T$add_rd_rm(rd, rm) /* add rd, rm */ \
    (0x4400 | (((rd) & 0x8) >> 3 << 7) | (((rm) & 0x8) >> 3 << 6) | (((rm) & 0x7) << 3) | ((rd) & 0x7))
#define T$push_r(r) /* push r... */ \
    (0xb400 | (((r) & (1 << A$lr)) >> A$lr << 8) | ((r) & 0xff))
#define T$pop_r(r) /* pop r... */ \
    (0xbc00 | (((r) & (1 << A$pc)) >> A$pc << 8) | ((r) & 0xff))
#define T$mov_rd_rm(rd, rm) /* mov rd, rm */ \
    (0x4600 | (((rd) & 0x8) >> 3 << 7) | (((rm) & 0x8) >> 3 << 6) | (((rm) & 0x7) << 3) | ((rd) & 0x7))
#define T$ldr_rd_$rn_im_4$(rd, rn, im) /* ldr rd, [rn, #im * 4] */ \
    (0x6800 | (((im) & 0x1f) << 6) | ((rn) << 3) | (rd))
#define T$ldr_rd_$pc_im_4$(rd, im) /* ldr rd, [PC, #im * 4] */ \
    (0x4800 | ((rd) << 8) | ((im) & 0xff))
#define T$cmp_rn_$im(rn, im) /* cmp rn, #im */ \
    (0x2000 | ((rn) << 8) | ((im) & 0xff))
#define T$it$_cd(cd, ms) /* it<ms>, cd */ \
    (0xbf00 | ((cd) << 4) | (ms))
#define T$cbz$_rn_$im(op,rn,im) /* cb<op>z rn, #im */ \
    (0xb100 | ((op) << 11) | (((im) & 0x40) >> 6 << 9) | (((im) & 0x3e) >> 1 << 3) | (rn))
#define T$b$_$im(cond,im) /* b<cond> #im */ \
    (cond == A$al ? 0xe000 | (((im) >> 1) & 0x7ff) : 0xd000 | ((cond) << 8) | (((im) >> 1) & 0xff))

#define T1$ldr_rt_$rn_im$(rt, rn, im) /* ldr rt, [rn, #im] */ \
    (0xf850 | ((im < 0 ? 0 : 1) << 7) | (rn))

template<class T> T xabs(T _Val)
{
        typedef int BOOL;
        if(_Val>T(0))return _Val;
        return -_Val;
}

#define T2$ldr_rt_$rn_im$(rt, rn, im) /* ldr rt, [rn, #im] */ \
    (((rt) << 12) | xabs(im))

#define T1$mrs_rd_apsr(rd) /* mrs rd, apsr */ \
    (0xf3ef)
#define T2$mrs_rd_apsr(rd) /* mrs rd, apsr */ \
    (0x8000 | ((rd) << 8))

#define T1$msr_apsr_nzcvqg_rn(rn) /* msr apsr, rn */ \
    (0xf380 | (rn))
#define T2$msr_apsr_nzcvqg_rn(rn) /* msr apsr, rn */ \
    (0x8c00)
#define T$msr_apsr_nzcvqg_rn(rn) /* msr apsr, rn */ \
    (T2$msr_apsr_nzcvqg_rn(rn) << 16 | T1$msr_apsr_nzcvqg_rn(rn))

static inline bool A$pcrel$r(uint32_t ic) {
    return (ic & 0x0c000000) == 0x04000000 && (ic & 0xf0000000) != 0xf0000000 && (ic & 0x000f0000) == 0x000f0000;
}

static inline bool T$32bit$i(uint16_t ic) {
    return ((ic & 0xe000) == 0xe000 && (ic & 0x1800) != 0x0000);
}

static inline bool T$pcrel$cbz(uint16_t ic) {
    return (ic & 0xf500) == 0xb100;
}

static inline bool T$pcrel$b(uint16_t ic) {
    return (ic & 0xf000) == 0xd000 && (ic & 0x0e00) != 0x0e00;
}

static inline bool T2$pcrel$b(uint16_t *ic) {
    return (ic[0] & 0xf800) == 0xf000 && ((ic[1] & 0xd000) == 0x9000 || (ic[1] & 0xd000) == 0x8000 && (ic[0] & 0x0380) != 0x0380);
}

static inline bool T$pcrel$bl(uint16_t *ic) {
    return (ic[0] & 0xf800) == 0xf000 && ((ic[1] & 0xd000) == 0xd000 || (ic[1] & 0xd001) == 0xc000);
}

static inline bool T$pcrel$ldr(uint16_t ic) {
    return (ic & 0xf800) == 0x4800;
}

static inline bool T$pcrel$add(uint16_t ic) {
    return (ic & 0xff78) == 0x4478;
}

static inline bool T$pcrel$ldrw(uint16_t ic) {
    return (ic & 0xff7f) == 0xf85f;
}

static size_t MSGetInstructionWidthThumb(void *start) {
    uint16_t *thumb(reinterpret_cast<uint16_t *>(start));
    return T$32bit$i(thumb[0]) ? 4 : 2;
}

static size_t MSGetInstructionWidthARM(void *start) {
    return 4;
}

extern "C" size_t MSGetInstructionWidth(void *start) {
    if ((reinterpret_cast<uintptr_t>(start) & 0x1) == 0)
        return MSGetInstructionWidthARM(start);
    else
        return MSGetInstructionWidthThumb(reinterpret_cast<void *>(reinterpret_cast<uintptr_t>(start) & ~0x1));
}

static size_t SubstrateHookFunctionThumb(SubstrateProcessRef process, void *symbol, void *replace, void **result) {
    if (symbol == NULL)
        return 0;
printf("SubstrateHookFunctionThumb\n");
    uint16_t *area(reinterpret_cast<uint16_t *>(symbol));

    unsigned align((reinterpret_cast<uintptr_t>(area) & 0x2) == 0 ? 0 : 1);
    uint16_t *thumb(area + align);

    uint32_t *arm(reinterpret_cast<uint32_t *>(thumb + 2));
    uint16_t *trail(reinterpret_cast<uint16_t *>(arm + 2));

    if (
        (align == 0 || area[0] == T$nop) &&
        thumb[0] == T$bx(A$pc) &&
        thumb[1] == T$nop &&
        arm[0] == A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8)
    ) {
        if (result != NULL)
            *result = reinterpret_cast<void *>(arm[1]);

        SubstrateHookMemory code(process, arm + 1, sizeof(uint32_t) * 1);

        arm[1] = reinterpret_cast<uint32_t>(replace);

        return sizeof(arm[0]);
    }

    size_t required((trail - area) * sizeof(uint16_t));

    size_t used(0);
    while (used < required)
        used += MSGetInstructionWidthThumb(reinterpret_cast<uint8_t *>(area) + used);
    used = (used + sizeof(uint16_t) - 1) / sizeof(uint16_t) * sizeof(uint16_t);

    size_t blank((used - required) / sizeof(uint16_t));

    uint16_t backup[used / sizeof(uint16_t)];
    memcpy(backup, area, used);

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", area);
        MSLogHexEx(area, used + sizeof(uint16_t), 2, name);
    }

    if (result != NULL) {

    size_t length(used);
    for (unsigned offset(0); offset != used / sizeof(uint16_t); ++offset)
        if (T$pcrel$ldr(backup[offset]))
            length += 3 * sizeof(uint16_t);
        else if (T$pcrel$b(backup[offset]))
            length += 6 * sizeof(uint16_t);
        else if (T2$pcrel$b(backup + offset)) {
            length += 5 * sizeof(uint16_t);
            ++offset;
        } else if (T$pcrel$bl(backup + offset)) {
            length += 5 * sizeof(uint16_t);
            ++offset;
        } else if (T$pcrel$cbz(backup[offset])) {
            length += 16 * sizeof(uint16_t);
        } else if (T$pcrel$ldrw(backup[offset])) {
            length += 4 * sizeof(uint16_t);
            ++offset;
        } else if (T$pcrel$add(backup[offset]))
            length += 6 * sizeof(uint16_t);
        else if (T$32bit$i(backup[offset]))
            ++offset;

    unsigned pad((length & 0x2) == 0 ? 0 : 1);
    length += (pad + 2) * sizeof(uint16_t) + 2 * sizeof(uint32_t);

    uint16_t *buffer(reinterpret_cast<uint16_t *>(mmap(
        NULL, length, PROT_READ | PROT_WRITE, MAP_ANON | MAP_PRIVATE, -1, 0
    )));

    if (buffer == MAP_FAILED) {
        MSLog(MSLogLevelError, "MS:Error:mmap() = %d", errno);
        *result = NULL;
        return 0;
    }

    if (false) fail: {
        munmap(buffer, length);
        *result = NULL;
        return 0;
    }

    size_t start(pad), end(length / sizeof(uint16_t));
    uint32_t *trailer(reinterpret_cast<uint32_t *>(buffer + end));
    for (unsigned offset(0); offset != used / sizeof(uint16_t); ++offset) {
        if (T$pcrel$ldr(backup[offset])) {
            union {
                uint16_t value;

                struct {
                    uint16_t immediate : 8;
                    uint16_t rd : 3;
                    uint16_t : 5;
                };
            } bits = {backup[offset+0]};

            buffer[start+0] = T$ldr_rd_$pc_im_4$(bits.rd, T$Label(start+0, end-2) / 4);
            buffer[start+1] = T$ldr_rd_$rn_im_4$(bits.rd, bits.rd, 0);

            // XXX: this code "works", but is "wrong": the mechanism is more complex than this
            *--trailer = ((reinterpret_cast<uint32_t>(area + offset) + 4) & ~0x2) + bits.immediate * 4;

            start += 2;
            end -= 2;
        } else if (T$pcrel$b(backup[offset])) {
            union {
                uint16_t value;

                struct {
                    uint16_t imm8 : 8;
                    uint16_t cond : 4;
                    uint16_t /*1101*/ : 4;
                };
            } bits = {backup[offset+0]};

            intptr_t jump(bits.imm8 << 1);
            jump |= 1;
            jump <<= 23;
            jump >>= 23;

            buffer[start+0] = T$b$_$im(bits.cond, (end-6 - (start+0)) * 2 - 4);

            *--trailer = reinterpret_cast<uint32_t>(area + offset) + 4 + jump;
            *--trailer = A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8);
            *--trailer = T$nop << 16 | T$bx(A$pc);

            start += 1;
            end -= 6;
        } else if (T2$pcrel$b(backup + offset)) {
            union {
                uint16_t value;

                struct {
                    uint16_t imm6 : 6;
                    uint16_t cond : 4;
                    uint16_t s : 1;
                    uint16_t : 5;
                };
            } bits = {backup[offset+0]};

            union {
                uint16_t value;

                struct {
                    uint16_t imm11 : 11;
                    uint16_t j2 : 1;
                    uint16_t a : 1;
                    uint16_t j1 : 1;
                    uint16_t : 2;
                };
            } exts = {backup[offset+1]};

            intptr_t jump(1);
            jump |= exts.imm11 << 1;
            jump |= bits.imm6 << 12;

            if (exts.a) {
                jump |= bits.s << 24;
                jump |= (~(bits.s ^ exts.j1) & 0x1) << 23;
                jump |= (~(bits.s ^ exts.j2) & 0x1) << 22;
                jump |= bits.cond << 18;
                jump <<= 7;
                jump >>= 7;
            } else {
                jump |= bits.s << 20;
                jump |= exts.j2 << 19;
                jump |= exts.j1 << 18;
                jump <<= 11;
                jump >>= 11;
            }

            buffer[start+0] = T$b$_$im(exts.a ? A$al : bits.cond, (end-6 - (start+0)) * 2 - 4);

            *--trailer = reinterpret_cast<uint32_t>(area + offset) + 4 + jump;
            *--trailer = A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8);
            *--trailer = T$nop << 16 | T$bx(A$pc);

            ++offset;
            start += 1;
            end -= 6;
        } else if (T$pcrel$bl(backup + offset)) {
            union {
                uint16_t value;

                struct {
                    uint16_t immediate : 10;
                    uint16_t s : 1;
                    uint16_t : 5;
                };
            } bits = {backup[offset+0]};

            union {
                uint16_t value;

                struct {
                    uint16_t immediate : 11;
                    uint16_t j2 : 1;
                    uint16_t x : 1;
                    uint16_t j1 : 1;
                    uint16_t : 2;
                };
            } exts = {backup[offset+1]};

            int32_t jump(0);
            jump |= bits.s << 24;
            jump |= (~(bits.s ^ exts.j1) & 0x1) << 23;
            jump |= (~(bits.s ^ exts.j2) & 0x1) << 22;
            jump |= bits.immediate << 12;
            jump |= exts.immediate << 1;
            jump |= exts.x;
            jump <<= 7;
            jump >>= 7;

            buffer[start+0] = T$push_r(1 << A$r7);
            buffer[start+1] = T$ldr_rd_$pc_im_4$(A$r7, ((end-2 - (start+1)) * 2 - 4 + 2) / 4);
            buffer[start+2] = T$mov_rd_rm(A$lr, A$r7);
            buffer[start+3] = T$pop_r(1 << A$r7);
            buffer[start+4] = T$blx(A$lr);

            *--trailer = reinterpret_cast<uint32_t>(area + offset) + 4 + jump;

            ++offset;
            start += 5;
            end -= 2;
        } else if (T$pcrel$cbz(backup[offset])) {
            union {
                uint16_t value;

                struct {
                    uint16_t rn : 3;
                    uint16_t immediate : 5;
                    uint16_t : 1;
                    uint16_t i : 1;
                    uint16_t : 1;
                    uint16_t op : 1;
                    uint16_t : 4;
                };
            } bits = {backup[offset+0]};

            intptr_t jump(1);
            jump |= bits.i << 6;
            jump |= bits.immediate << 1;

            //jump <<= 24;
            //jump >>= 24;

            unsigned rn(bits.rn);
            unsigned rt(rn == A$r7 ? A$r6 : A$r7);

            buffer[start+0] = T$push_r(1 << rt);
            buffer[start+1] = T1$mrs_rd_apsr(rt);
            buffer[start+2] = T2$mrs_rd_apsr(rt);
            buffer[start+3] = T$cbz$_rn_$im(bits.op, rn, (end-10 - (start+3)) * 2 - 4);
            buffer[start+4] = T1$msr_apsr_nzcvqg_rn(rt);
            buffer[start+5] = T2$msr_apsr_nzcvqg_rn(rt);
            buffer[start+6] = T$pop_r(1 << rt);

            *--trailer = reinterpret_cast<uint32_t>(area + offset) + 4 + jump;
            *--trailer = A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8);
            *--trailer = T$nop << 16 | T$bx(A$pc);
            *--trailer = T$nop << 16 | T$pop_r(1 << rt);
            *--trailer = T$msr_apsr_nzcvqg_rn(rt);

#if 0
            if ((start & 0x1) == 0)
                buffer[start++] = T$nop;
            buffer[start++] = T$bx(A$pc);
            buffer[start++] = T$nop;

            uint32_t *arm(reinterpret_cast<uint32_t *>(buffer + start));
            arm[0] = A$add(A$lr, A$pc, 1);
            arm[1] = A$ldr_rd_$rn_im$(A$pc, A$pc, (trailer - arm) * sizeof(uint32_t) - 8);
#endif

            start += 7;
            end -= 10;
        } else if (T$pcrel$ldrw(backup[offset])) {
            union {
                uint16_t value;

                struct {
                    uint16_t : 7;
                    uint16_t u : 1;
                    uint16_t : 8;
                };
            } bits = {backup[offset+0]};

            union {
                uint16_t value;

                struct {
                    uint16_t immediate : 12;
                    uint16_t rt : 4;
                };
            } exts = {backup[offset+1]};

            buffer[start+0] = T1$ldr_rt_$rn_im$(exts.rt, A$pc, T$Label(start+0, end-2));
            buffer[start+1] = T2$ldr_rt_$rn_im$(exts.rt, A$pc, T$Label(start+0, end-2));

            buffer[start+2] = T1$ldr_rt_$rn_im$(exts.rt, exts.rt, 0);
            buffer[start+3] = T2$ldr_rt_$rn_im$(exts.rt, exts.rt, 0);

            // XXX: this code "works", but is "wrong": the mechanism is more complex than this
            *--trailer = ((reinterpret_cast<uint32_t>(area + offset) + 4) & ~0x2) + (bits.u == 0 ? -exts.immediate : exts.immediate);

            ++offset;
            start += 4;
            end -= 2;
        } else if (T$pcrel$add(backup[offset])) {
            union {
                uint16_t value;

                struct {
                    uint16_t rd : 3;
                    uint16_t rm : 3;
                    uint16_t h2 : 1;
                    uint16_t h1 : 1;
                    uint16_t : 8;
                };
            } bits = {backup[offset+0]};

            if (bits.h1) {
                MSLog(MSLogLevelError, "MS:Error:pcrel(%u):add (rd > r7)", offset);
                goto fail;
            }

            unsigned rt(bits.rd == A$r7 ? A$r6 : A$r7);

            buffer[start+0] = T$push_r(1 << rt);
            buffer[start+1] = T$mov_rd_rm(rt, (bits.h1 << 3) | bits.rd);
            buffer[start+2] = T$ldr_rd_$pc_im_4$(bits.rd, T$Label(start+2, end-2) / 4);
            buffer[start+3] = T$add_rd_rm((bits.h1 << 3) | bits.rd, rt);
            buffer[start+4] = T$pop_r(1 << rt);
            *--trailer = reinterpret_cast<uint32_t>(area + offset) + 4;

            start += 5;
            end -= 2;
        } else if (T$32bit$i(backup[offset])) {
            buffer[start++] = backup[offset];
            buffer[start++] = backup[++offset];
        } else {
            buffer[start++] = backup[offset];
        }
    }

    buffer[start++] = T$bx(A$pc);
    buffer[start++] = T$nop;

    uint32_t *transfer = reinterpret_cast<uint32_t *>(buffer + start);
    transfer[0] = A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8);
    transfer[1] = reinterpret_cast<uint32_t>(area + used / sizeof(uint16_t)) + 1;

    if (mprotect(buffer, length, PROT_READ | PROT_EXEC) == -1) {
        MSLog(MSLogLevelError, "MS:Error:mprotect():%d", errno);
        return 0;
    }

    *result = reinterpret_cast<uint8_t *>(buffer + pad) + 1;

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", *result);
        MSLogHexEx(buffer, length, 2, name);
    }

    }

    {
        SubstrateHookMemory code(process, area, used);

        if (align != 0)
            area[0] = T$nop;

        thumb[0] = T$bx(A$pc);
        thumb[1] = T$nop;

        arm[0] = A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8);
        arm[1] = reinterpret_cast<uint32_t>(replace);

        for (unsigned offset(0); offset != blank; ++offset)
            trail[offset] = T$nop;
    }

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", area);
        MSLogHexEx(area, used + sizeof(uint16_t), 2, name);
    }
	
	return used;
}

static size_t SubstrateHookFunctionARM(SubstrateProcessRef process, void *symbol, void *replace, void **result) {
    if (symbol == NULL)
        return 0;
printf("SubstrateHookFunctionARM\n");
    uint32_t *area(reinterpret_cast<uint32_t *>(symbol));
    uint32_t *arm(area);

    const size_t used(8);

    uint32_t backup[used / sizeof(uint32_t)] = {arm[0], arm[1]};

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", area);
        MSLogHexEx(area, used + sizeof(uint32_t), 4, name);
    }

    if (result != NULL) {

    if (backup[0] == A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8)) {
        *result = reinterpret_cast<void *>(backup[1]);
        
		return sizeof(backup[0]);
    }

    size_t length(used);
    for (unsigned offset(0); offset != used / sizeof(uint32_t); ++offset)
        if (A$pcrel$r(backup[offset])) {
            if ((backup[offset] & 0x02000000) == 0 || (backup[offset] & 0x0000f000 >> 12) != (backup[offset] & 0x0000000f))
                length += 2 * sizeof(uint32_t);
            else
                length += 4 * sizeof(uint32_t);
        }

    length += 2 * sizeof(uint32_t);

    uint32_t *buffer(reinterpret_cast<uint32_t *>(mmap(
        NULL, length, PROT_READ | PROT_WRITE, MAP_ANON | MAP_PRIVATE, -1, 0
    )));

    if (buffer == MAP_FAILED) {
        MSLog(MSLogLevelError, "MS:Error:mmap() = %d", errno);
        *result = NULL;
        return 0;
    }

    if (false) fail: {
        munmap(buffer, length);
        *result = NULL;
        return 0;
    }

    size_t start(0), end(length / sizeof(uint32_t));
    uint32_t *trailer(reinterpret_cast<uint32_t *>(buffer + end));
    for (unsigned offset(0); offset != used / sizeof(uint32_t); ++offset)
        if (A$pcrel$r(backup[offset])) {
            union {
                uint32_t value;

                struct {
                    uint32_t rm : 4;
                    uint32_t : 1;
                    uint32_t shift : 2;
                    uint32_t shiftamount : 5;
                    uint32_t rd : 4;
                    uint32_t rn : 4;
                    uint32_t l : 1;
                    uint32_t w : 1;
                    uint32_t b : 1;
                    uint32_t u : 1;
                    uint32_t p : 1;
                    uint32_t mode : 1;
                    uint32_t type : 2;
                    uint32_t cond : 4;
                };
            } bits = {backup[offset+0]}, copy(bits);

            bool guard;
            if (bits.mode == 0 || bits.rd != bits.rm) {
                copy.rn = bits.rd;
                guard = false;
            } else {
                copy.rn = bits.rm != A$r0 ? A$r0 : A$r1;
                guard = true;
            }

            if (guard)
                buffer[start++] = A$stmdb_sp$_$rs$((1 << copy.rn));

            buffer[start+0] = A$ldr_rd_$rn_im$(copy.rn, A$pc, (end-1 - (start+0)) * 4 - 8);
            buffer[start+1] = copy.value;

            start += 2;

            if (guard)
                buffer[start++] = A$ldmia_sp$_$rs$((1 << copy.rn));

            *--trailer = reinterpret_cast<uint32_t>(area + offset) + 8;
            end -= 1;
        } else
            buffer[start++] = backup[offset];

    buffer[start+0] = A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8);
    buffer[start+1] = reinterpret_cast<uint32_t>(area + used / sizeof(uint32_t));

    if (mprotect(buffer, length, PROT_READ | PROT_EXEC) == -1) {
        MSLog(MSLogLevelError, "MS:Error:mprotect():%d", errno);
        goto fail;
    }

    *result = buffer;

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", *result);
        MSLogHexEx(buffer, length, 4, name);
    }

    }

    {
        SubstrateHookMemory code(process, symbol, used);

        arm[0] = A$ldr_rd_$rn_im$(A$pc, A$pc, 4 - 8);
        arm[1] = reinterpret_cast<uint32_t>(replace);
    }

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", area);
        MSLogHexEx(area, used + sizeof(uint32_t), 4, name);
    }
	
	return used;
}

static size_t SubstrateHookFunction(SubstrateProcessRef process, void *symbol, void *replace, void **result) {
    if (MSDebug)
        MSLog(MSLogLevelNotice, "SubstrateHookFunction(%p, %p, %p, %p)\n", process, symbol, replace, result);
    if ((reinterpret_cast<uintptr_t>(symbol) & 0x1) == 0)
        return SubstrateHookFunctionARM(process, symbol, replace, result);
    else
        return SubstrateHookFunctionThumb(process, reinterpret_cast<void *>(reinterpret_cast<uintptr_t>(symbol) & ~0x1), replace, result);
}
#endif

#if defined(__i386__) || defined(__x86_64__)

#include "SubstrateX86.hpp"

static size_t MSGetInstructionWidthIntel(void *start) {
    hde64s decode;
    return hde64_disasm(start, &decode);
}

static void SubstrateHookFunction(SubstrateProcessRef process, void *symbol, void *replace, void **result) {
    if (MSDebug)
        MSLog(MSLogLevelNotice, "MSHookFunction(%p, %p, %p)\n", symbol, replace, result);
    if (symbol == NULL)
        return;

    uintptr_t source(reinterpret_cast<uintptr_t>(symbol));
    uintptr_t target(reinterpret_cast<uintptr_t>(replace));

    uint8_t *area(reinterpret_cast<uint8_t *>(symbol));

    size_t required(MSSizeOfJump(target, source));

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", area);
        MSLogHex(area, 32, name);
    }

    size_t used(0);
    while (used < required) {
        size_t width(MSGetInstructionWidthIntel(area + used));
        if (width == 0) {
            MSLog(MSLogLevelError, "MS:Error:MSGetInstructionWidthIntel(%p) == 0", area + used);
            return;
        }

        used += width;
    }

    size_t blank(used - required);

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", area);
        MSLogHex(area, used + sizeof(uint16_t), name);
    }

    uint8_t backup[used];
    memcpy(backup, area, used);

    if (result != NULL) {

    if (backup[0] == 0xe9) {
        *result = reinterpret_cast<void *>(source + 5 + *reinterpret_cast<uint32_t *>(backup + 1));
        return;
    }

    if (!ia32 && backup[0] == 0xff && backup[1] == 0x25) {
        *result = *reinterpret_cast<void **>(source + 6 + *reinterpret_cast<uint32_t *>(backup + 2));
        return;
    }

    size_t length(used + MSSizeOfJump(source + used));

    for (size_t offset(0), width; offset != used; offset += width) {
        hde64s decode;
        hde64_disasm(backup + offset, &decode);
        width = decode.len;
        //_assert(width != 0 && offset + width <= used);

#ifdef __LP64__
        if ((decode.modrm & 0xc7) == 0x05) {
            if (decode.opcode == 0x8b) {
                void *destiny(area + offset + width + int32_t(decode.disp.disp32));
                uint8_t reg(decode.rex_r << 3 | decode.modrm_reg);
                length -= decode.len;
                length += MSSizeOfPushPointer(destiny);
                length += MSSizeOfPop(reg);
                length += MSSizeOfMove64();
            } else {
                MSLog(MSLogLevelError, "MS:Error: Unknown RIP-Relative (%.2x %.2x)", decode.opcode, decode.opcode2);
                continue;
            }
        } else
#endif

        if (backup[offset] == 0xe8) {
            int32_t relative(*reinterpret_cast<int32_t *>(backup + offset + 1));
            void *destiny(area + offset + decode.len + relative);

            if (relative == 0) {
                length -= decode.len;
                length += MSSizeOfPushPointer(destiny);
            } else {
                length += MSSizeOfSkip();
                length += MSSizeOfJump(destiny);
            }
        } else if (backup[offset] == 0xeb) {
            length -= decode.len;
            length += MSSizeOfJump(area + offset + decode.len + *reinterpret_cast<int8_t *>(backup + offset + 1));
        } else if (backup[offset] == 0xe9) {
            length -= decode.len;
            length += MSSizeOfJump(area + offset + decode.len + *reinterpret_cast<int32_t *>(backup + offset + 1));
        } else if (
            backup[offset] == 0xe3 ||
            (backup[offset] & 0xf0) == 0x70
            // XXX: opcode2 & 0xf0 is 0x80?
        ) {
            length += decode.len;
            length += MSSizeOfJump(area + offset + decode.len + *reinterpret_cast<int8_t *>(backup + offset + 1));
        }
    }

    uint8_t *buffer(reinterpret_cast<uint8_t *>(mmap(
        NULL, length, PROT_READ | PROT_WRITE, MAP_ANON | MAP_PRIVATE, -1, 0
    )));

    if (buffer == MAP_FAILED) {
        MSLog(MSLogLevelError, "MS:Error:mmap() = %d", errno);
        *result = NULL;
        return;
    }

    if (false) fail: {
        munmap(buffer, length);
        *result = NULL;
        return;
    }

    {
        uint8_t *current(buffer);

        for (size_t offset(0), width; offset != used; offset += width) {
            hde64s decode;
            hde64_disasm(backup + offset, &decode);
            width = decode.len;
            //_assert(width != 0 && offset + width <= used);

#ifdef __LP64__
            if ((decode.modrm & 0xc7) == 0x05) {
                if (decode.opcode == 0x8b) {
                    void *destiny(area + offset + width + int32_t(decode.disp.disp32));
                    uint8_t reg(decode.rex_r << 3 | decode.modrm_reg);
                    MSPushPointer(current, destiny);
                    MSWritePop(current, reg);
                    MSWriteMove64(current, reg, reg);
                } else {
                    MSLog(MSLogLevelError, "MS:Error: Unknown RIP-Relative (%.2x %.2x)", decode.opcode, decode.opcode2);
                    goto copy;
                }
            } else
#endif

            if (backup[offset] == 0xe8) {
                int32_t relative(*reinterpret_cast<int32_t *>(backup + offset + 1));
                if (relative == 0)
                    MSPushPointer(current, area + offset + decode.len);
                else {
                    MSWrite<uint8_t>(current, 0xe8);
                    MSWrite<int32_t>(current, MSSizeOfSkip());
                    void *destiny(area + offset + decode.len + relative);
                    MSWriteSkip(current, MSSizeOfJump(destiny, current + MSSizeOfSkip()));
                    MSWriteJump(current, destiny);
                }
            } else if (backup[offset] == 0xeb)
                MSWriteJump(current, area + offset + decode.len + *reinterpret_cast<int8_t *>(backup + offset + 1));
            else if (backup[offset] == 0xe9)
                MSWriteJump(current, area + offset + decode.len + *reinterpret_cast<int32_t *>(backup + offset + 1));
            else if (
                backup[offset] == 0xe3 ||
                (backup[offset] & 0xf0) == 0x70
            ) {
                MSWrite<uint8_t>(current, backup[offset]);
                MSWrite<uint8_t>(current, 2);
                MSWrite<uint8_t>(current, 0xeb);
                void *destiny(area + offset + decode.len + *reinterpret_cast<int8_t *>(backup + offset + 1));
                MSWrite<uint8_t>(current, MSSizeOfJump(destiny, current + 1));
                MSWriteJump(current, destiny);
            } else
#ifdef __LP64__
                copy:
#endif
            {
                MSWrite(current, backup + offset, width);
            }
        }

        MSWriteJump(current, area + used);
    }

    if (mprotect(buffer, length, PROT_READ | PROT_EXEC) == -1) {
        MSLog(MSLogLevelError, "MS:Error:mprotect():%d", errno);
        goto fail;
    }

    *result = buffer;

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", *result);
        MSLogHex(buffer, length, name);
    }

    }

    {
        SubstrateHookMemory code(process, area, used);

        uint8_t *current(area);
        MSWriteJump(current, target);
        for (unsigned offset(0); offset != blank; ++offset)
            MSWrite<uint8_t>(current, 0x90);
    }

    if (MSDebug) {
        char name[16];
        sprintf(name, "%p", area);
        MSLogHex(area, used + sizeof(uint16_t), name);
    }
}
#endif

_extern void MSHookFunction(void *symbol, void *replace, void **result) {
#ifndef __LP64__
    SubstrateHookFunction(NULL, symbol, replace, result);
#endif
}

#if defined(__APPLE__) && defined(__arm__)
_extern void _Z14MSHookFunctionPvS_PS_(void *symbol, void *replace, void **result) {
    return MSHookFunction(symbol, replace, result);
}
#endif
