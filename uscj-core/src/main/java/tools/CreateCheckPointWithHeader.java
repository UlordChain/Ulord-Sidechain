package tools;

import co.usc.ulordj.core.*;
import co.usc.ulordj.params.TestNet3Params;

import javax.annotation.Nullable;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.TreeMap;

public class CreateCheckPointWithHeader {

    private static final NetworkParameters params = TestNet3Params.get();

    private static final String[] headersHexStr = {
            /* 2000*/"00000020295210a34938777e9befce6ce25b51c9eab714a70bb436c943daa95afe02000094e432c02ec6ba06c4dec7513248a0f05807074b89723907656c6be8cfac40cc0000000000000000000000000000000000000000000000000000000000000000f322dc5a851a051e8e5555152e5b44628304ef411dff0d46a4f401379b791e139b4ba0365f18109b",
            /* 4000*/"01000020c428eb5c12fbc412e4fcfc7d37f3aae1236e8a0023c9c9cdb6f487fbfb00000002e2ed04e6aa404b32b412433c49639bc4044affe5f1988d7dd9f81c944ea3b1000000000000000000000000000000000000000000000000000000000000000098b9e05af2f2041ebb851cc7aa9bbc2fac454094a3ee2ac9a2a0ef5172b547941cc08ebc4e06edf4",
            /* 6000*/"010000204b3f86320ccdc164087cb2e64115129e69f9c58f870a5d6a87164811a70200008171d079c272c6b5b95b7327316d6ed3dde81ccc3817ee27698660e82fb6775a00000000000000000000000000000000000000000000000000000000000000004d49e55a75b1031e43a09959aea0255e171be7728d6683b4977931b122b002c2c8b173a2f20853a8",
            /* 8000*/"00000020b227c92e9281768692b5ab71a1fb20fc908df1d679218db7d3b1c468f6040000f8e818598cf9a2b321e5828f0b543c4b258613858bfdfada2c5e138539bc2c9b0000000000000000000000000000000000000000000000000000000000000000def6e95a89ac071e57120000f9fe62f576cc650015bd07f86c2c62f1530c0176114ab01195c8cf0f",
            /*10000*/"000000204f76b352a6f18e33e5da6932d8b3ea9bd6999425cd8d743afb0f16a351010000cf5484fca8a1fbb619bd1ee9dd09dbfff939c39676b0c36e94afc034c404499d00000000000000000000000000000000000000000000000000000000000000003c7dee5a9bff021e5e010040afa9f719bbcd71ef24246d53a5863550c949386a0e5cd8e8312fef1e",
            /*12000*/"00000020f9d1ac69e3906cd9cf9cde3731abca3f5bda59100755e1640b313b686f0100007a9fd5f22a3677b1ba481e345d8ba42cc625437a9224bc743427d7929c1806c500000000000000000000000000000000000000000000000000000000000000009c08f35aebcc011e86000000fecf90019067523965905f6e00ad347b43baddf4d1e319f8b466dd1c",
    };

    private static final String headerHexStr = "00000020c0dfd584a7ae3e45cce22058ec254b0c6d9418c9724f13b6a0f3e9bb0a010000d7a03593417d968d90b5b9d1e4a852741b5552631f56d8937b2746b638881bca0000000000000000000000000000000000000000000000000000000000000000734cf55ac49b011e021900480511f9ac9838365424df85011b7e70e70a2dbaee9e63e36ee389887e";
    private static final Integer height = 12987;

    public static void main(@Nullable String[] args) {

        TreeMap<Integer, StoredBlock> checkpoints = new TreeMap<>();
        for(int i = 0; i < headersHexStr.length; ++i) {
            byte[] bBytes = Sha256Hash.hexStringToByteArray(headersHexStr[i]);
            UldBlock block = new UldBlock(params, bBytes);
            StoredBlock storedBlock;
            switch (i)
            {
                case 5:
                    storedBlock = new StoredBlock(block, new BigInteger("00000000000000000000000000000000000000000000000000000000a24ba896", 16), 2000);
                    break;
                case 4:
                    storedBlock = new StoredBlock(block, new BigInteger("0000000000000000000000000000000000000000000000000000000266a2e396", 16), 4000);
                    break;
                case 3:
                    storedBlock = new StoredBlock(block, new BigInteger("00000000000000000000000000000000000000000000000000000004c31b7844", 16), 6000);
                    break;
                case 2:
                    storedBlock = new StoredBlock(block, new BigInteger("0000000000000000000000000000000000000000000000000000000659c398d1", 16), 8000);
                    break;
                case 1:
                    storedBlock = new StoredBlock(block, new BigInteger("00000000000000000000000000000000000000000000000000000000a24ba896", 16), 10000);
                    break;
                case 0:
                    storedBlock = new StoredBlock(block, new BigInteger("0000000000000000000000000000000000000000000000000000000c4944a6d5", 16), 12000);
                    break;
                default:
                    storedBlock = new StoredBlock(block, new BigInteger("00000000000000000000000000000000000000000000000000000000a24ba896", 16), 2000);
                    break;
            }
            checkpoints.put(storedBlock.getHeight(), storedBlock);

        }

//        byte[] blockBytes = Sha256Hash.hexStringToByteArray(headerHexStr);
//        UldBlock block= new UldBlock(params, blockBytes);
//        StoredBlock storedBlock =  new StoredBlock(block, new BigInteger("0000000000000000000000000000000000000000000000000000001325ac3572", 16), height);
//        checkpoints.put(storedBlock.getHeight(), storedBlock);

        File textFile = new File("org.ulord.test.checkpoints");

        writeBinaryCheckpoints(checkpoints, textFile);
        //writeTextualCheckpoints(checkpoints, textFile);
    }

    private static void writeTextualCheckpoints(TreeMap<Integer, StoredBlock> checkpoints, File file) {
        try {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.US_ASCII));
            writer.println("TXT CHECKPOINTS 1");
            writer.println("0"); // Number of signatures to read. Do this later.
            writer.println(checkpoints.size());
            ByteBuffer buffer = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
            for (StoredBlock block : checkpoints.values()) {
                block.serializeCompact(buffer);
                writer.println(CheckpointManager.BASE64.encode(buffer.array()));
                buffer.position(0);
            }
            writer.close();
            System.out.println("Checkpoints written to '" + file.getCanonicalPath() + "'.");
        }  catch (IOException ex) {
            System.out.println("Exception: " + ex);
        }
    }

    private static void writeBinaryCheckpoints(TreeMap<Integer, StoredBlock> checkpoints, File file) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            MessageDigest digest = Sha256Hash.newDigest();
            final DigestOutputStream digestOutputStream = new DigestOutputStream(fileOutputStream, digest);
            digestOutputStream.on(false);
            final DataOutputStream dataOutputStream = new DataOutputStream(digestOutputStream);
            dataOutputStream.writeBytes("CHECKPOINTS 1");
            dataOutputStream.writeInt(0);  // Number of signatures to read. Do this later.
            digestOutputStream.on(true);
            dataOutputStream.writeInt(checkpoints.size());
            ByteBuffer buffer = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
            for (StoredBlock block : checkpoints.values()) {
                block.serializeCompact(buffer);
                dataOutputStream.write(buffer.array());
                buffer.position(0);
            }
            dataOutputStream.close();
            Sha256Hash checkpointsHash = Sha256Hash.wrap(digest.digest());
            System.out.println("Hash of checkpoints data is " + checkpointsHash);
            digestOutputStream.close();
            fileOutputStream.close();
            System.out.println("Checkpoints written to '" + file.getCanonicalPath() + "'.");

        }  catch (IOException ex) {
            System.out.println("Exception: " + ex);
        }
    }
}
