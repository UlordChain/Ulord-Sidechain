# Welcome to Ulord-Sidechain

## About
Ulord-Sidechain a.k.a USC is a secondary chain for Ulord implemented in java. USC's goal is providing the value and functionality to the Ulord ecosystem by enabling smart-contracts, near instant payment confirmations and higher-scalability. USC  smart contract platform  supports 2-way peg with Ulord that also rewards Ulord miners via merge-mining, allowing them to actively participate in the Smart Contract revolution.
 

## Sidechain techonology  
Sidechain is not specifically referring to a certain chain, but refers to any chain that obeys the rules of the sidechain. The Ulord sidechain rule means that the Ulord sidechain can verify the data from the Ulord main chain, and through the Two-way Peg (2WP), the UlordToken is safely transferred between the mainchains and sidechains at a fixed exchange rate to achieve assets transfer between the chains.  

### The Two-way Peg is roughly divided into the following stages:  
*	Send a 2WP transaction to lock the UlordToken in the main chain;  
*	Wait for a confirmation period, making the transaction confirmed by enough blocks;  
*	Transfer the UlordToken to the sidechain and provide proof of SPV or miner's vote;  
*	Wait for a reorganization period to prevent double spending;  
*	Unlock the UlordToken for normal use on the sidechain;  

After a period of time, if the user wants to redeem the UlordToken to the main chain, the reverse action can be performed. The redemption operation provides a sidechain user with an exit mechanism to prevent users from being forced to bind assets to unwanted sidechain applications.  
 ![The Two-way Peg between Ulord main chain and sidechain](https://github.com/UlordChain/Ulord-Sidechain/blob/master/pics/Two-way-Peg.jpg)

Each sidechain can operate in different networks, with independent economic patterns and corresponding DAPPs. Developers can construct a sidechain and then dock it to the Ulord main chain. While inheriting and reusing the Ulord main chain technology, they also share the pressure of the Ulord main chain.  

Each DAPP deployed on the sidechain is allowed to have a unique set of ledgers. According to different application scenarios of the DAPP, the sidechain's consensus mechanism and block parameters are allowed to be reformed. And because the sidechain is an independent system, the serious problem of DAPP on the sidechain will only affect the sidechain itself and will not affect the Ulord main chain.  

Ulord supports multiple sidechains, each of which can support one or more DAPPs. Sidechains can have their own virtual machines, publish smart contracts, and remain compatible with Ethereum virtual machines.

## Installation 
Ulord-Sidechain has a dependency of ulordj-thin, which contains the proof of work algorithm. In order to install Ulord-Sidechain we first need to download ulordj-thin and run 'make' from /hello directory contained inside src directory. The 'make' command will generate a lib file in the /usr/lib directory. 
NOTE: Currently Ulord-Sidechain is only supported in Linux and will be available for windows soon.

Next we need to download Pre-requisites
 1. [Java 8 JDK](http://www.webupd8.org/2012/09/install-oracle-java-8-in-ubuntu-via-ppa.html) 	Follow the steps to install Java. To check if installation went correctly, check the version with command: java-version. Then, as admin, modify /etc/enviroment adding JAVA_HOME="/usr/lib/jvm/java-8-oracle"
 2. [IntelliJ IDEA Community](https://www.jetbrains.com/idea/download/#section=linux)	Install this Java IDE

### Get external dependencies
Before you can launch IntelliJ IDEA, there is an important step. Browse in your Ulord-Sidechain cloned directory and then launch configure.sh with the following terminal command: ./configure.sh. This will download and set important components (e.g. Gradle Wrapper).

When IntelliJ IDEA is launched you should have a window with different choices.

1. Choose Import project.
2. Browse in the USC downloaded code the file usc\build.gradle and select it. Click NEXT.
3. Within the dialog select Use default gradle wrapper and then click Finish. Keep IntelliJ IDEA opened.

EA Build/Run configuration
We need to create a new configuration profile to run the node from IDEA. Part of the configuration information that needs to be provided is related to the node itself, you can find a sample configuration file on: Ulord-Sidechain/ulord-testnet.conf. Some settings need to be initialized.

Next step is to create a new configuration profile on IDEA, that can be done by clicking on Run -> Edit Configurations. 
We need to set the following fields
Main Class: co.usc.Start
VM Options: -Dusc.conf.file=/path-to-usc-conf/ulord-testnet.conf
Working directory: /path-to-code/Ulord-Sidechain
Use classpath of module: uscj-core_main

Congratulations! Now you are ready to build and run the node...
JRE need to be set as: Default (1.8 - SDK of 'usc-core_main' module)
# Getting Started
Information about compiling and running an USC node can be found in the [wiki](https://github.com/UlordChain/Ulord-Sidechain/wiki).
# License
USC is licensed under the GNU Lesser General Public License v3.0, also included in our repository in the [COPYING.LESSER](https://github.com/UlordChain/Ulord-Sidechain/blob/master/COPYING.LESSER) file.
