#!/bin/bash

#
# before run this script install git, debhelper, lintian and dpkg-dev
#

if [ -z "$1" ]; then
	echo "Argument usc node jar not supplied"
	exit 2
elif [ -f "$1" ]; then
	FILE_NAME_NODE=$1
else
	echo "usc node jar not exists"
	exit 2
fi

nativeLib="libCryptoHello.so"
if [ ! -f "$nativeLib" ]; then
    echo "libCryptoHello.so file not exists"
    exit 2
fi

if [ -z "$2" ]; then
	echo "Argument version not supplied"
	exit 2
else
	VERSION=$2
fi

RFCDATE=$(date --rfc-2822)

USER=$(whoami)
HOME=$(eval echo "~$USER")
WORKSPACE=$(echo "$HOME/$VERSION")
SOURCEDIR=~/packaging/debian

mkdir -p $WORKSPACE/source

mkdir -p $WORKSPACE/source/{bionic,xenial}/usc_$VERSION

cp -r $SOURCEDIR/usc_package_bionic/. $WORKSPACE/source/bionic/usc_$VERSION/
cp -r $SOURCEDIR/usc_package_xenial/. $WORKSPACE/source/xenial/usc_$VERSION/


sed -i "s|<V>|${VERSION}|g" $WORKSPACE/source/bionic/usc_$VERSION/debian/control
sed -i "s|<V>|${VERSION}|g" $WORKSPACE/source/bionic/usc_$VERSION/debian/changelog
sed -i "s|<DATE>|${RFCDATE}|g" $WORKSPACE/source/bionic/usc_$VERSION/debian/changelog

sed -i "s|<V>|${VERSION}|g" $WORKSPACE/source/xenial/usc_$VERSION/debian/control
sed -i "s|<V>|${VERSION}|g" $WORKSPACE/source/xenial/usc_$VERSION/debian/changelog
sed -i "s|<DATE>|${RFCDATE}|g" $WORKSPACE/source/xenial/usc_$VERSION/debian/changelog

zip $FILE_NAME_NODE libCryptoHello.so
cp $FILE_NAME_NODE $WORKSPACE/source/bionic/usc_$VERSION/src/usc.jar
cp $FILE_NAME_NODE $WORKSPACE/source/xenial/usc_$VERSION/src/usc.jar

#cp $nativeLib $WORKSPACE/source/bionic/usc_$VERSION/src/libCryptoHello.so
#cp $nativeLib $WORKSPACE/source/xenial/usc_$VERSION/src/libCryptoHello.so

cp $SOURCEDIR/config/regtest.conf $WORKSPACE/source/bionic/usc_$VERSION/src/regtest.conf
cp $SOURCEDIR/config/mainnet.conf $WORKSPACE/source/bionic/usc_$VERSION/src/mainnet.conf
cp $SOURCEDIR/config/testnet.conf $WORKSPACE/source/bionic/usc_$VERSION/src/testnet.conf
cp $SOURCEDIR/config/logback.xml $WORKSPACE/source/bionic/usc_$VERSION/src/
cp $SOURCEDIR/init-scripts/usc.service-node $WORKSPACE/source/bionic/usc_$VERSION/src/usc.service

cp $SOURCEDIR/config/testnet.conf $WORKSPACE/source/xenial/usc_$VERSION/src/testnet.conf
cp $SOURCEDIR/config/regtest.conf $WORKSPACE/source/xenial/usc_$VERSION/src/regtest.conf
cp $SOURCEDIR/config/mainnet.conf $WORKSPACE/source/xenial/usc_$VERSION/src/mainnet.conf
cp $SOURCEDIR/config/logback.xml $WORKSPACE/source/xenial/usc_$VERSION/src/
cp $SOURCEDIR/init-scripts/usc.service-node $WORKSPACE/source/xenial/usc_$VERSION/src/usc.service

echo "####################################################"
echo "#                   BUILD BIONIC                    #"
echo "####################################################"
cd $WORKSPACE/source/bionic/usc_$VERSION
debuild -us -uc -S

echo "####################################################"
echo "#                   BUILD XENIAL                   #"
echo "####################################################"
cd $WORKSPACE/source/xenial/usc_$VERSION
debuild -us -uc -S


#cd $WORKSPACE/source/xenial

mkdir -p $WORKSPACE/build/{xenial,bionic}

mv $WORKSPACE/source/xenial/usc_$VERSION* $WORKSPACE/build/xenial/
mv $WORKSPACE/source/bionic/usc_$VERSION* $WORKSPACE/build/bionic/
