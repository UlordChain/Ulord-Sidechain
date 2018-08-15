#!/bin/bash

GRADLE_WRAPPER="0f49043be582d7a39b671f924c66bd9337b92fa88ff5951225acc60560053067"
DOWNLOADED_HASH2=''
DOWNLOAD_FILE2=$(dd if=/dev/urandom bs=64 count=1 2>/dev/null| od -t x8 -A none  | tr -d ' '\\n)
unamestr=`uname`


function downloadJar(){
	platform
	if [ ! -d ./uscj-core/libs ]; then
		mkdir ./uscj-core/libs
	fi
	curl https://deps.rsklabs.io/gradle-wrapper.jar -o ~/$DOWNLOAD_FILE2
    if [[ $PLATFORM == 'linux' || $PLATFORM == 'windows' ]]; then
        DOWNLOADED_HASH2=$(sha256sum ~/${DOWNLOAD_FILE2} | cut -d' ' -f1)
    elif [[ $PLATFORM == 'mac' ]]; then
        DOWNLOADED_HASH2=$(shasum -a 256 ~/${DOWNLOAD_FILE2} | cut -d' ' -f1)
    fi

    if [[ $GRADLE_WRAPPER != $DOWNLOADED_HASH2 ]]; then
        rm -f ~/${DOWNLOAD_FILE2}
        exit 1
    else
        mv ~/${DOWNLOAD_FILE2} ./gradle/wrapper/gradle-wrapper.jar
        rm -f ~/${DOWNLOAD_FILE2}
    fi

	curl -o ./uscj-core/libs/ulordj-thin-0.0.1-usc-1-bundled.jar -L https://github.com/UlordChain/ulordj-thin/releases/download/ulordj-thin-0.0.1/ulordj-thin-0.0.1-usc-1-bundled.jar;

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