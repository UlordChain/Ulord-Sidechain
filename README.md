# Welcome to Ulord-Sidechain

## About
Ulord-Sidechain a.k.a USC is a Secondary chain for Ulord implemented in java. USC's goal is to provide value and functionality to the Ulord ecosystem by enabling smart-contracts, near instant payment confirmations and higher-scalability. USC  smart contract platform  supports 2-way peg with Ulord that also rewards Ulord miners via merge-mining, allowing them to actively participate in the Smart Contract revolution.
 

## Sidechain techonology  
Sidechain is not specifically referring to a certain chain, but refers to any chain that obeys the rules of the sidechain. The Ulord SideChain Rule (USCR) means that the USC can verify the data from the Ulord main chain, and through the Two-way Peg (2WP), the UlordToken is safely transferred between the mainchains and sidechains at a fixed exchange rate to achieve assets transfer between the chains.  

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



