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

#ifndef SUBSTRATE_BUFFER_HPP
#define SUBSTRATE_BUFFER_HPP

#include <string.h>

template <typename Type_>
_disused static _finline void MSWrite(uint8_t *&buffer, Type_ value) {
    *reinterpret_cast<Type_ *>(buffer) = value;
    buffer += sizeof(Type_);
}

_disused static _finline void MSWrite(uint8_t *&buffer, uint8_t *data, size_t size) {
    memcpy(buffer, data, size);
    buffer += size;
}

#endif//SUBSTRATE_BUFFER_HPP
