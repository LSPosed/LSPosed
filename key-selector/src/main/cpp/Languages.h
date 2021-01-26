//
// Created by 双草酸酯 on 1/26/21.
//

#ifndef EDXPOSED_LANGUAGES_H
#define EDXPOSED_LANGUAGES_H
#include <string>

class Languages {
public:
    virtual const std::string desc_line_1() {
        return "Select variant. Use Volume Down to move and Volume Up to confirm.";
    }
    virtual const std::string desc_line_2(int seconds) {
        const char base[] = "The program will select YAHFA for you in %d seconds if you don't \nhave a physical volume button. ";
        std::string out;
        out.resize(sizeof(base) + 20);
        sprintf(out.data(), base, seconds);
        return out;
    }
};

class LanguageChinese: public Languages {
public:
    const std::string desc_line_1() override {
        return "请选择变种。使用音量减切换，音量加确定。";
    }
    const std::string desc_line_2(int seconds) override {
        const char base[] = "如果您的设备没有音量键，本程序将会在 %d 秒后自动选择 YAHFA 。";
        std::string out;
        out.resize(sizeof(base) + 20);
        sprintf(out.data(), base, seconds);
        return out;
    }
};

#endif //EDXPOSED_LANGUAGES_H
