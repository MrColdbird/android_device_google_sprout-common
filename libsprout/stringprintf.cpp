/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdarg.h>
#include <string>

namespace android {
	// These printf-like functions are implemented in terms of vsnprintf, so they
	// use the same attribute for compile-time format string checking. On Windows,
	// if the mingw version of vsnprintf is used, use `gnu_printf' which allows z
	// in %zd and PRIu64 (and related) to be recognized by the compile-time
	// checking.
	#define FORMAT_ARCHETYPE __printf__
	#ifdef __USE_MINGW_ANSI_STDIO
	#if __USE_MINGW_ANSI_STDIO
	#undef FORMAT_ARCHETYPE
	#define FORMAT_ARCHETYPE gnu_printf
	#endif
	#endif
	// Returns a string corresponding to printf-like formatting of the arguments.
	std::string StringPrintf(const char* fmt, ...)
		__attribute__((__format__(FORMAT_ARCHETYPE, 1, 2)));
	// Appends a printf-like formatting of the arguments to 'dst'.
	void StringAppendF(std::string* dst, const char* fmt, ...)
		__attribute__((__format__(FORMAT_ARCHETYPE, 2, 3)));
	// Appends a printf-like formatting of the arguments to 'dst'.
	void StringAppendV(std::string* dst, const char* format, va_list ap)
		__attribute__((__format__(FORMAT_ARCHETYPE, 2, 0)));
	#undef FORMAT_ARCHETYPE
	
	void StringAppendV(std::string* dst, const char* format, va_list ap) {
		// First try with a small fixed size buffer
		char space[1024];
		// It's possible for methods that use a va_list to invalidate
		// the data in it upon use.  The fix is to make a copy
		// of the structure before using it and use that copy instead.
		va_list backup_ap;
		va_copy(backup_ap, ap);
		int result = vsnprintf(space, sizeof(space), format, backup_ap);
		va_end(backup_ap);
		if (result < static_cast<int>(sizeof(space))) {
			if (result >= 0) {
				// Normal case -- everything fit.
				dst->append(space, result);
				return;
			}
			if (result < 0) {
				// Just an error.
				return;
			}
		}
		// Increase the buffer size to the size requested by vsnprintf,
		// plus one for the closing \0.
		int length = result + 1;
		char* buf = new char[length];
		// Restore the va_list before we use it again
		va_copy(backup_ap, ap);
		result = vsnprintf(buf, length, format, backup_ap);
		va_end(backup_ap);
		if (result >= 0 && result < length) {
			// It fit
			dst->append(buf, result);
		}
		delete[] buf;
	}
	
	std::string StringPrintf(const char* fmt, ...) {
		va_list ap;
		va_start(ap, fmt);
		std::string result;
		StringAppendV(&result, fmt, ap);
		va_end(ap);
		return result;
	}
	
	void StringAppendF(std::string* dst, const char* format, ...) {
		va_list ap;
		va_start(ap, format);
		StringAppendV(dst, format, ap);
		va_end(ap);
	}
}  // namespace android
