package tools;

import co.usc.core.Usc;
import org.ethereum.vm.PrecompiledContracts;

import javax.xml.crypto.Data;
import java.io.IOException;

import static tools.UscRpc.*;
import static tools.Utils.getGasForTx;
import static tools.Utils.getMinimumGasPrice;

public class FederationManagement {

    private static final String BRIDGE_ADDRESS = PrecompiledContracts.BRIDGE_ADDR_STR;

    public static void main(String[] args) {

        if(args.length < 1) {
            help();
            return;
        }

        try {
            String response = "";

            switch (args[0]) {
                case "getFederationAddress":
                    response = getFederationAddress();
                    response = DataDecoder.decodeGetFederationAddress(response);
                    break;
                case "getFederationSize":
                    response = getFederationSize();
                    response = DataDecoder.decodeGetFederationSize(response).toString();
                    break;
                case "getFederationThreshold":
                    response = getFederationThreshold();
                    response = DataDecoder.decodeGetFederationThreshold(response).toString();
                    break;
                case "getFederatorPublicKeys":
                    response = getFederatorPublicKeys();
                    break;
                case "getFederationCreationTime":
                    response = getFederationCreationTime();
                    response = DataDecoder.decodeGetFederationCreationTime(response).toString();
                    break;
                case "getFederationCreationBlockNumber":
                    response = getFederationCreationBlockNumber();
                    response = DataDecoder.decodeGetFederationCreationBlockNumber(response).toString();
                    break;

                case "getRetiringFederationAddress":
                    response = getRetiringFederationAddress();
                    response = DataDecoder.decodeGetRetiringFederationAddress(response);
                    break;
                case "getRetiringFederationSize":
                    response = getRetiringFederationSize();
                    response = DataDecoder.decodeGetRetiringFederationSize(response).toString();
                    break;
                case "getRetiringFederationThreshold":
                    response = getRetiringFederationThreshold();
                    response = DataDecoder.decodeGetRetiringFederationThreshold(response).toString();
                    break;
                case "getRetiringFederatorPublicKeys":
                    response = getRetiringFederatorPublicKeys();
                    break;
                case "getRetiringFederationCreationTime":
                    response = getRetiringFederationCreationTime();
                    response = DataDecoder.decodeGetRetiringFederationCreationTime(response).toString();
                    break;
                case "getRetiringFederationCreationBlockNumber":
                    response = getRetiringFederationCreationBlockNumber();
                    response = DataDecoder.decodeGetRetiringFederationCreationBlockNumber(response).toString();
                    break;

                case "createFederation":
                    if(args.length < 3) {
                        System.out.println("createFederation <ChangeAuthorizedAddress> <UserPassword>");
                        return;
                    }
                    response = createFederation(args[1], args[2]);
                    break;
                case "addFederatorPublicKey":
                    if(args.length < 4) {
                        System.out.println("addFederatorPublicKey <ChangeAuthorizedAddress> <UserPassword> <New Federator Publickey>");
                        return;
                    }
                    response = addFederatorPublicKey(args[1], args[2], args[3]);
                    break;
                case "commitFederation":
                    if(args.length < 4) {
                        System.out.println("commitFederation <ChangeAuthorizedAddress> <UserPassword> <Pending Federation Hash>");
                        return;
                    }
                    response = commitFederation(args[1], args[2], args[3]);
                    break;
                case "rollbackFederation":
                    if(args.length < 3) {
                        System.out.println("rollbackFederation <ChangeAuthorizedAddress> <UserPassword>");
                        return;
                    }
                    response = rollackFederation(args[1], args[2]);
                    break;

                case "getPendingFederationHash":
                    response = getPendingFederationHash();
                    break;
                case "getPendingFederationSize":
                    response = getPendingFederationSize();
                    break;
                case "getPendingFederatorPublicKeys":
                    response = getPendingFederatorPublicKeys();
                    break;

                default:
                    help();
                    break;
            }
            if(response.isEmpty())
                response = null;
            System.out.println(response);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static String getRetiringFederatorPublicKeys() throws IOException {
        int size = DataDecoder.decodeGetRetiringFederationSize(UscRpc.getRetiringFederationSize());
        if(size < 0) {
            return null;
        }
        StringBuilder publicKeys = new StringBuilder();
        for (int i = 0; i < size; i++) {
            String res = UscRpc.getRetiringFederatorPublicKey(i);
            String pubKey = DataDecoder.decodeGetRetiringFederatorPublicKey(res);
            publicKeys.append(pubKey);
            publicKeys.append("\n");
        }
        return publicKeys.toString();
    }

    private static String getFederatorPublicKeys() throws IOException{
        int size = DataDecoder.decodeGetFederationSize(UscRpc.getFederationSize());
        if(size < 0) {
            return null;
        }
        StringBuilder publicKeys = new StringBuilder();
        for (int i = 0; i < size; i++) {
            String res = UscRpc.getFederationPublicKey(i);
            String pubKey = DataDecoder.decodeGetFederatorPublicKey(res);
            publicKeys.append(pubKey);
            publicKeys.append("\n");
        }
        return publicKeys.toString();
    }

    private static String getPendingFederationSize() throws IOException {
        return UscRpc.call(BRIDGE_ADDRESS, DataEncoder.encodeGetPendingFederationSize());
    }

    private static String getPendingFederatorPublicKeys() throws IOException {
        int size = DataDecoder.decodeGetPendingFederationSize(getPendingFederationSize());
        if(size < 0) {
            return null;
        }
        StringBuilder publicKeys = new StringBuilder();
        for (int i = 0; i < size; i++) {
            String res = UscRpc.getPendingFederationPublicKey(i);
            String pubkey = DataDecoder.decodeGetPendingFederatorPublicKey(res);
            publicKeys.append(pubkey);
            publicKeys.append("\n");
        }
        return publicKeys.toString();
    }

    private static void help() {
        System.out.println("<function name> <param1> <param2> ...");
        System.out.println("Federation Management:" + "\n"
                        + "    Creating a federation requires to follow these steps:" + "\n"
                        + "        1. createFederation: Creates a Pending Federation" + "\n"
                        + "        2. addFederatorPublicKey: Adds the Ulord Federator public key" + "\n"
                        + "        3. getPendingFederationHash: returns the Pending Federation's Hash" + "\n"
                        + "        4. commitFederation: Once all the Federator's public keys are added with sufficient vote the Federation can by committed" + "\n"
                        + "        *. rollbackFederation: Removes the Pending Federation" + "\n"
                        + "\n"
                        + "------------------------------------------------------------------------------------------------------------------------------------" + "\n"
                        + "   Functions:                           Parameters" + "\n"
                        + "getFederationAddress" + "\n"
                        + "getFederationSize" + "\n"
                        + "getFederationThreshold" + "\n"
                        + "getFederationPublicKeys" + "\n"
                        + "getFederationCreationTime" + "\n"
                        + "getFederationCreationBlockNumber" + "\n"
                        + "getRetiringFederationAddress" + "\n"
                        + "getRetiringFederationSize" + "\n"
                        + "getRetiringFederationThreshold" + "\n"
                        + "getRetiringFederationPublicKeys" + "\n"
                        + "getRetiringFederationCreationTime" + "\n"
                        + "getRetiringFederationCreationBlockNumber" + "\n"
                        + "createFederation                     <ChangeAuthorizedAddress> <UserPassword>" + "\n"
                        + "addFederatorPublicKey                <ChangeAuthorizedAddress> <UserPassword> <New Federator Publickey>" + "\n"
                        + "commitFederation                     <ChangeAuthorizedAddress> <UserPassword> <Pending Federation Hash>" + "\n"
                        + "rollbackFederation                   <ChangeAuthorizedAddress> <UserPassword>" + "\n"
                        + "getPendingFederationHash" + "\n"
                        + "getPendingFederatorPublicKeys" + "\n"
                        + "getPendingFederationSize" + "\n"
                        + "------------------------------------------------------------------------------------------------------------------------------------" + "\n"
        );
    }

    private static String createFederation(String fedChangeAuthKey, String usrPwd) throws IOException, PrivateKeyNotFoundException {
        if(!Utils.tryUnlockUscAccount(fedChangeAuthKey, usrPwd))
            throw new PrivateKeyNotFoundException();

        String gasPrice = getMinimumGasPrice();
        String gas = getGasForTx(fedChangeAuthKey, BRIDGE_ADDRESS, null, gasPrice, null, DataEncoder.encodeCreateFederation(), null);
        return UscRpc.sendTransaction(fedChangeAuthKey, BRIDGE_ADDRESS, gas, gasPrice, null, DataEncoder.encodeCreateFederation(), null);
    }

    private static String addFederatorPublicKey(String fedChangeAuthKey, String usrPwd, String publicKey) throws IOException, PrivateKeyNotFoundException {
        if(!Utils.tryUnlockUscAccount(fedChangeAuthKey, usrPwd))
            throw new PrivateKeyNotFoundException();

        return UscRpc.sendTransaction(fedChangeAuthKey, BRIDGE_ADDRESS, null, getMinimumGasPrice(), null, DataEncoder.encodeAddFederatorPublicKey(publicKey), null);
    }

    private static String getPendingFederationHash() throws IOException {
        String res = UscRpc.call(BRIDGE_ADDRESS, DataEncoder.encodeGetPendingFederationHash());
        return DataDecoder.decodeGetPendingFederationHash(res);
    }

    private static String commitFederation(String fedChangeAuthKey, String usrPwd, String pendingFedHash) throws IOException, PrivateKeyNotFoundException {
        if(!Utils.tryUnlockUscAccount(fedChangeAuthKey, usrPwd))
            throw new PrivateKeyNotFoundException();

        String gasPrice = getMinimumGasPrice();
        String gas = getGasForTx(fedChangeAuthKey, BRIDGE_ADDRESS, null, gasPrice, null, DataEncoder.encodeCommitFederation(pendingFedHash), null);
        return UscRpc.sendTransaction(fedChangeAuthKey, BRIDGE_ADDRESS, gas, gasPrice, null, DataEncoder.encodeCommitFederation(pendingFedHash), null);
    }

    private static String rollackFederation(String fedChangeAuthKey, String usrPwd)  throws IOException, PrivateKeyNotFoundException {
        if(!Utils.tryUnlockUscAccount(fedChangeAuthKey, usrPwd))
            throw new PrivateKeyNotFoundException();

        String gasPrice = getMinimumGasPrice();
        String gas = getGasForTx(fedChangeAuthKey, BRIDGE_ADDRESS, null, gasPrice, null, DataEncoder.encodeRollbackFederation(), null);
        return UscRpc.sendTransaction(fedChangeAuthKey, BRIDGE_ADDRESS, gas, gasPrice, null, DataEncoder.encodeRollbackFederation(), null);
    }


}
