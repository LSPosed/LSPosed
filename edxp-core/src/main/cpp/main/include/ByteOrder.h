//
// Created by solo on 2019/3/24.
//

#ifndef EDXPOSED_TEMP_BYTEORDER_H
#define EDXPOSED_TEMP_BYTEORDER_H

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

#endif //EDXPOSED_TEMP_BYTEORDER_H
