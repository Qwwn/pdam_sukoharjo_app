cmake_minimum_required(VERSION 3.22.1)

# Add this line with your project name
project("tirtamakmur")

add_library(
        native-lib
        SHARED
        native-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        native-lib
        ${log-lib}
)