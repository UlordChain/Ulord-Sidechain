package tools;

import co.usc.core.Usc;
import org.ethereum.vm.PrecompiledContracts;

import java.io.IOException;

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
                    response = commitFederation(args[1], args[2], args[3]);
                    break;
                case "rollbackFederation":
                    response = rollackFederation(args[1], args[2]);
                    break;
                case "getPendingFederationHash":
                    response = getPendingFederationHash();
                    break;
                default:
                    help();
                    break;
            }

            System.out.println(response);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void help() {
        System.out.println("<function name> <param1> <param2> ...");
        System.out.println("Federation Management:" + "\n"
                         + "    Creating a federation requires to follow these steps:" + "\n"
                         + "        1. createFederation: Creates a Pending Federation" + "\n"
                         + "        2. addFederatorPublicKey: Adds the Ulord Federator public key" + "\n"
                         + "        3. getPendingFederationHash: returns the Pending Federation's Hash" + "\n"
                         + "        4. commitFederation: Once all the Federator's public keys are added with sufficient vote the Federation can by committed" + "\n"
                         + "        *. rollbackFederation: Removes the Pending Federation"
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
        return UscRpc.call(BRIDGE_ADDRESS, DataEncoder.encodeGetPendingFederationHash());
    }

    private static String commitFederation(String fedChangeAuthKey, String usrPwd, String pendingFedHash) throws IOException, PrivateKeyNotFoundException {
        if(!Utils.tryUnlockUscAccount(fedChangeAuthKey, usrPwd))
            throw new PrivateKeyNotFoundException();

        String gasPrice = getMinimumGasPrice();
        String gas = getGasForTx(fedChangeAuthKey, BRIDGE_ADDRESS, null, gasPrice, null, DataEncoder.encodeAddFederatorPublicKey(pendingFedHash), null);
        return UscRpc.sendTransaction(fedChangeAuthKey, BRIDGE_ADDRESS, gas, gasPrice, null, DataEncoder.encodeAddFederatorPublicKey(pendingFedHash), null);
    }

    private static String rollackFederation(String fedChangeAuthKey, String usrPwd)  throws IOException, PrivateKeyNotFoundException {
        if(!Utils.tryUnlockUscAccount(fedChangeAuthKey, usrPwd))
            throw new PrivateKeyNotFoundException();

        String gasPrice = getMinimumGasPrice();
        String gas = getGasForTx(fedChangeAuthKey, BRIDGE_ADDRESS, null, gasPrice, null, DataEncoder.encodeRollbackFederation(), null);
        return UscRpc.sendTransaction(fedChangeAuthKey, BRIDGE_ADDRESS, gas, gasPrice, null, DataEncoder.encodeRollbackFederation(), null);
    }


}
