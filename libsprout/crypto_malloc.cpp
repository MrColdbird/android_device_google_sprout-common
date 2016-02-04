#include <malloc.h>

extern "C" void *CRYPTO_malloc(int num, const char *file, int line) {
	return malloc(num);
}

extern "C" void CRYPTO_free(void *buf) {
	free(buf);
}
