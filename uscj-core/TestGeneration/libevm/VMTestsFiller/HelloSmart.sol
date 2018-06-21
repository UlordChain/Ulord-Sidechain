pragma solidity ^0.4.4;

contract HelloSmart {
    string greeting;
    
    function HelloSmart() {
        greeting = "Hello smart world!";
    }
    
    function getGreeting ()  returns (uint greetingStr) {
        return 10153;
    }
    
    function setGreeting(string _newGreeting) returns (bool success) {
        greeting = _newGreeting;
        return true;
    }
}









