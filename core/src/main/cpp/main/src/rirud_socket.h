//
// Created by loves on 12/2/2020.
//
#pragma once

#include <string>
#include <filesystem>


class RirudSocket {
public:
//    class DirIter {};

    class RirudSocketException : public std::runtime_error {
    public:
        RirudSocketException(const std::string &what) : std::runtime_error(what) {}
    };

    RirudSocket();

    std::string ReadFile(const std::filesystem::path &path);

//    DirIter ReadDir(const std::filesystem::path &path);
//    DirIter RecursiveReadDir(const std::filesystem::path &path);

    ~RirudSocket();

private:
    RirudSocket(const RirudSocket &) = delete;

    RirudSocket operator=(const RirudSocket &) = delete;


    inline static const uint32_t ACTION_READ_FILE = 4;

    template<typename T>
    void Write(const T &);

    template<typename T>
    void Read(T &);

    void Write(const char *buf, size_t len) const;

    void Read(char *buf, size_t len) const;

    int fd_ = -1;
};
