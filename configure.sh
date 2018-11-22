#!/bin/bash

unamestr=`uname`


function downloadJar(){
	platform
	if [ ! -d ./uscj-core/libs ]; then
		mkdir ./uscj-core/libs
	fi

    if [[ $PLATFORM == 'linux'  ]]; then
        sudo curl -o /usr/lib/libCryptoHello.so -L https://github.com/UlordChain/ulordj-thin/releases/download/ulordj-thin-0.0.1/libCryptoHello.so
    elif [[ $PLATFORM == 'windows' ]]; then
        echo "Platform currently not supported"
    elif [[ $PLATFORM == 'mac' ]]; then
        echo "Platform currently not supported"
    fi

    curl -o ./uscj-core/libs/ulordj-thin-0.0.1-usc-1-bundled.jar -L https://github.com/UlordChain/ulordj-thin/releases/download/ulordj-thin-0.0.1/ulordj-thin-0.0.1-usc-1-bundled.jar;
    curl -o ./uscj-core/libs/lll-compiler-0.0.2.jar -L https://github.com/UlordChain/lll-compiler/releases/download/0.0.2/lll-compiler-0.0.2.jar
    curl -o ./uscj-core/libs/bclcrypto-jdk15on-1.59.jar -L https://github.com/UlordChain/lll-compiler/releases/download/0.0.2/bclcrypto-jdk15on-1.59.jar
}

function platform() {
	if [[ "$unamestr" == 'Linux' ]]; then
		PLATFORM='linux'
	elif [[ "$unamestr" == 'Darwin' ]]; then
		PLATFORM='mac'
	elif [[ "$unamestr" =~ 'MINGW' ]]; then
		PLATFORM='windows'
	else
			echo -e "\e[1m\e[31m[ ERROR ]\e[0m UNRECOGNIZED PLATFORM"
			exit 2
	fi
}

downloadJar

exit 0
