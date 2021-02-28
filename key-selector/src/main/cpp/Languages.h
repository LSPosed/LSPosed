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

//
// Created by 双草酸酯 on 1/26/21.
//

#ifndef LSPOSED_LANGUAGES_H
#define LSPOSED_LANGUAGES_H
#include <string>

class Languages {
public:
    virtual const std::string desc_line_1() {
        return "Select variant. Use Volume Down to move and Volume Up to confirm.";
    }
    virtual const std::string desc_line_2(const uint16_t seconds) {
        const char base[] = "The program will select YAHFA for you in %hu seconds if you don't \nhave a physical volume button. ";
        return u16fmt(base, seconds);
    }
    virtual const std::string timeout(const uint16_t seconds) {
        const char base[] = "No operation after %hu seconds.";
        return u16fmt(base, seconds);
    };
    virtual const std::string deprecated() {
        return "(Deprecated)";
    };
protected:
    std::string u16fmt(const char* base, std::uint16_t s){
        std::string out;
        out.resize(strlen(base) + 20);
        sprintf(out.data(), base, s);
        return out;
    }
};

class LanguageChinese: public Languages {
public:
    const std::string desc_line_1() override {
        return "请选择变种。使用音量减切换，音量加确定。";
    }
    const std::string desc_line_2(const uint16_t seconds) override {
        const char base[] = "如果您的设备没有音量键，本程序将会在 %hu 秒后自动选择 YAHFA 。";
        return u16fmt(base, seconds);
    }
    const std::string timeout(const uint16_t seconds) override {
        const char base[] = "在 %hu 秒内没有任何操作。";
        return u16fmt(base, seconds);
    }
    virtual const std::string deprecated() override {
        return "（已废弃）";
    };
};

#endif //LSPOSED_LANGUAGES_H
