cmake_minimum_required(VERSION 3.22.1)

project("campusgeo")

add_library(
        campusgeo
        SHARED
        src/main/cpp/native-geo.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        campusgeo
        ${log-lib}
)
