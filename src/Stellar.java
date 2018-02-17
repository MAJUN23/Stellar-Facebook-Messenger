import org.stellar.sdk.*;
import java.net.*;
import java.io.*;
import java.util.*;
import org.stellar.sdk.responses.*;
import org.stellar.sdk.requests.*;

class Stellar{

    public static KeyPair createKeyPair(){
        // Generates seed for creating a key pair
        KeyPair pair = KeyPair.random();
        System.out.println(pair.getSecretSeed());
        System.out.println(pair.getAccountId());
        return pair;

    }

    public static void registerTestNetAccount(KeyPair pair) throws Exception{
        // Registers account with the TestNet
        String friendBotUrl = String.format(
                "https://horizon-testnet.stellar.org/friendbot?addr=%s",
                pair.getAccountId());
        InputStream response = new URL(friendBotUrl).openStream();
        String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();
        System.out.println("SUCCESS! You have a new account :)\n" + body);

    }

    public static void getAccountInfo(Server server, KeyPair pair) throws IOException {
        AccountResponse account = server.accounts().account(pair); // throws IOException
        System.out.println("Balances for account " + pair.getAccountId());

        for (AccountResponse.Balance balance : account.getBalances()) {
            System.out.println(String.format(
                    "Type: %s, Code: %s, Balance: %s",
                    balance.getAssetType(),
                    balance.getAssetCode(),
                    balance.getBalance()));
        }
    }

    public static void main(String[] args) throws Exception{

        Network.useTestNetwork();
        Server server = new Server("https://horizon-testnet.stellar.org");
        String secretSourceSeed = "SBUVSDJFSK3457KNNUVWEJZ4MHKJ27XKMKYSV6GL3QWFTO5LP2GBKCOI";
        String destinationAccountId = "GDETWWDUX4AEC3ZXQ4Q7XISYB2ZTKWCGWETLUIALA2Y5RAG2E7SDWSCC";

        KeyPair source = KeyPair.fromSecretSeed(secretSourceSeed);
        KeyPair destination = KeyPair.fromAccountId(destinationAccountId);

        // Get balance account info for sender
        System.out.println("Sender Account Info: ");
        getAccountInfo(server, source);

        System.out.println("Receiver Account Info: ");
        getAccountInfo(server, destination);
    }

    public static void sendLumens(KeyPair source, KeyPair destination, Server server) throws Exception {
        // Checks if destination account exists
        // If account does not exist HttpResponseException will be thrown
        server.accounts().account(destination);
        // Load up to date information about
        AccountsRequestBuilder sourceAccountBuilder = server.accounts();

        // Throws HttpResponseException if error
        AccountResponse sourceAccount = sourceAccountBuilder.account(source);

        // Build the transaction
        Transaction.Builder transactionBuilder = new Transaction.Builder(sourceAccount);
        // AssetTypeNative represents the Stellar Lumens Native asset
        PaymentOperation payment = new PaymentOperation.Builder(
                destination, new AssetTypeNative(), "10").build();

        // Add metadata to transaction (optional)
        transactionBuilder.addMemo(Memo.text("Test transaction"));
        Transaction transaction = transactionBuilder.addOperation(payment).build();

        transaction.sign(source); // sign transaction with private seed

        // And finally, send it off to Stellar!
        int count = 0;
        while (count < 5) {
            try {
                count++;
                SubmitTransactionResponse response = server.submitTransaction(transaction);
                System.out.println("Success!");
                System.out.println(response);
                break;

            } catch (Exception e) {
                System.out.println("Something went wrong! Retrying...");
                System.out.println(e.getMessage());
                // TODO: Check for actual response from Horizon server. Resubmit if no reponse
            }

        }
    }

}