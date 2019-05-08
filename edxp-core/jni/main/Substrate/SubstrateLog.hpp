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

#ifndef SUBSTRATE_LOG_HPP
#define SUBSTRATE_LOG_HPP

#if 0
#include <android/log.h>

#define MSLog(level, format, ...) ((void)__android_log_print(level, "NNNN", format, __VA_ARGS__))

#define MSLogLevelNotice ANDROID_LOG_INFO
#define MSLogLevelWarning ANDROID_LOG_WARN
#define MSLogLevelError ANDROID_LOG_ERROR

#else

#define MSLog(level, format, ...) printf(format, __VA_ARGS__)

#endif

#endif//SUBSTRATE_LOG_HPP
