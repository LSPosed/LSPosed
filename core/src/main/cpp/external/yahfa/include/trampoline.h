//
// Created by liuruikai756 on 05/07/2017.
//

#ifndef YAHFA_TAMPOLINE_H
#define YAHFA_TAMPOLINE_H
#ifdef __cplusplus
extern "C" {
#endif
extern int SDKVersion;
extern size_t OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;

extern unsigned char trampoline[];

void setupTrampoline();
void *genTrampoline(void *hookMethod);
#ifdef __cplusplus
}
#endif
#endif //YAHFA_TAMPOLINE_H
