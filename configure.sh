#!/bin/bash

unamestr=`uname`

function downloadJar(){
	platform
	if [ ! -d ./uscj-core/libs ]; then
		mkdir ./uscj-core/libs
	fi
	curl -o ./uscj-core/libs/ulordj-thin-0.0.1-usc-1-bundled.jar -L https://github.com/UlordChain/ulordj-thin/releases/download/ulordj-thin-0.0.1/ulordj-thin-0.0.1-usc-1-bundled.jar
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