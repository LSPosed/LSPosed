/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#ifndef LSPOSED_TEMP_BYTEORDER_H
#define LSPOSED_TEMP_BYTEORDER_H

#include <cstdint>

static inline uint32_t android_swap_long(uint32_t v)
{
    return (v<<24) | ((v<<8)&0x00FF0000) | ((v>>8)&0x0000FF00) | (v>>24);
}
static inline uint16_t android_swap_short(uint16_t v)
{
    return (v<<8) | (v>>8);
}

#define DEVICE_BYTE_ORDER LITTLE_ENDIAN
#if BYTE_ORDER == DEVICE_BYTE_ORDER
#define	dtohl(x)	(x)
#define	dtohs(x)	(x)
#define	htodl(x)	(x)
#define	htods(x)	(x)
#else
#define	dtohl(x)	(android_swap_long(x))
#define	dtohs(x)	(android_swap_short(x))
#define	htodl(x)	(android_swap_long(x))
#define	htods(x)	(android_swap_short(x))
#endif
#if BYTE_ORDER == LITTLE_ENDIAN
#define fromlel(x) (x)
#define fromles(x) (x)
#define tolel(x) (x)
#define toles(x) (x)
#else
#define fromlel(x) (android_swap_long(x))
#define fromles(x) (android_swap_short(x))
#define tolel(x) (android_swap_long(x))
#define toles(x) (android_swap_short(x))
#endif

#endif //LSPOSED_TEMP_BYTEORDER_H
