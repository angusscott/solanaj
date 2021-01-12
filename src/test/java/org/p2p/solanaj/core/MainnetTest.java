package org.p2p.solanaj.core;

import org.bitcoinj.core.Utils;
import org.junit.Test;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.AccountInfo;
import org.p2p.solanaj.serum.*;
import org.p2p.solanaj.utils.ByteUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;

public class MainnetTest {

    private final RpcClient client = new RpcClient(Cluster.MAINNET);
    private final PublicKey publicKey = new PublicKey("skynetDj29GH6o6bAqoixCpDuYtWqi1rm8ZNx1hB3vq");

    public static final int initialized = 1;  // Binary 00000001
    public static final int market = 2;  // Binary 00000010
    public static final int openOrders = 4;  // Binary 00000100
    public static final int requestQueue = 8;  // Binary 00001000
    public static final int eventQueue = 16;  // Binary 00010000
    public static final int bids = 32;  // Binary 00100000
    public static final int asks = 64;  // Binary 01000000

    @Test
    public void getAccountInfoBase64() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(publicKey);
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Account data list
            final List<String> accountData = accountInfo.getValue().getData();

            // Verify "base64" string in accountData
            assertTrue(accountData.stream().anyMatch(s -> s.equalsIgnoreCase("base64")));
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAccountInfoBase58() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(publicKey, Map.of("encoding", "base58"));
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Account data list
            final List<String> accountData = accountInfo.getValue().getData();

            // Verify "base64" string in accountData
            assertTrue(accountData.stream().anyMatch(s -> s.equalsIgnoreCase("base58")));
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAccountInfoRootCommitment() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(publicKey, Map.of("commitment", "root"));
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Verify any balance
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void marketAccountTest() {
        try {
            // Pubkey of BTC/USDC market
            final PublicKey publicKey = new PublicKey("CVfYa8RGXnuDBeGmniCcdkBwoLqVxh92xB1JqgRQx3F"); //BTC/USDC
            //final PublicKey publicKey = new PublicKey("FrDavxi4QawYnQY259PVfYUjUvuyPNfqSXbLBqMnbfWJ"); //FIDA/USDC
            //final PublicKey publicKey = new PublicKey("3HZWXFCx74xapSPV4rqBv2V7jUshauGS37vqxxoGp6qJ"); //LQID/USDC


            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(publicKey);
            final List<String> accountData = accountInfo.getValue().getData();
            final String base64Data = accountData.get(0);

            // Deserialize market from the binary data
            if (base64Data != null) {
                byte[] bytes = Base64.getDecoder().decode(accountData.get(0));
                Market market = Market.readMarket(bytes);
                System.out.println(market.toString());

                // Deserialize the bid order book. This is just proof of concept - will be moved into classes.
                // If orderbook.dat exists, use it.
                byte[] data = new byte[0];

//                try {
//                    data = Files.readAllBytes(Paths.get("orderbook.dat"));
//                } catch (IOException e) {
//                    // e.printStackTrace();
//                }

                if (data.length == 0) {
                    AccountInfo bidAccount = client.getApi().getAccountInfo(market.getBids());
                    data = Base64.getDecoder().decode(bidAccount.getValue().getData().get(0));
                }

                OrderBook bidOrderBook = OrderBook.readOrderBook(data);
                market.setBidOrderBook(bidOrderBook);

                System.out.println(bidOrderBook.getAccountFlags().toString());

                System.out.println("BTC/USDC Bids Orderbook");
                bidOrderBook.getSlab().getSlabNodes().stream().sorted(Comparator.comparingLong(value -> {
                    if (value instanceof SlabLeafNode) {
                        return ((SlabLeafNode) value).getPrice();
                    }
                    return 0;
                }).reversed()).forEach(slabNode -> {
                    if (slabNode instanceof SlabLeafNode) {
                        SlabLeafNode slabLeafNode = (SlabLeafNode)slabNode;
                        System.out.println("Order: Bid " + slabLeafNode.getQuantity()/10000.0 + " BTC/USDC at $" + slabLeafNode.getPrice()/10);
                    }
                });

//                bidOrderBook.getSlab().getSlabNodes().forEach(slabNode -> {
//                    if (slabNode instanceof SlabLeafNode) {
//                        SlabLeafNode slabLeafNode = (SlabLeafNode)slabNode;
//                        System.out.println("Order: Bid " + slabLeafNode.getQuantity()/100000.0 + " BTC/USDC at $" + slabLeafNode.getPrice()/10);
//                    }
//                });

            }

            // Verify any balance
            assertTrue(true);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void orderBookTest() {
        byte[] data = new byte[0];

        try {
            data = Files.readAllBytes(Paths.get("orderbook.dat"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        OrderBook bidOrderBook = OrderBook.readOrderBook(data);

        System.out.println(bidOrderBook.getAccountFlags().toString());

        Slab slab = bidOrderBook.getSlab();

        assertNotNull(slab);
        assertEquals(141, slab.getBumpIndex());
        assertEquals(78, slab.getFreeListLen());
        assertEquals(56, slab.getFreeListHead());
        assertEquals(32, slab.getLeafCount());
    }

    @Test
    public void orderBook2Test() {
        byte[] data = new byte[0];

        try {
            data = Files.readAllBytes(Paths.get("orderbook2.dat"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        OrderBook bidOrderBook = OrderBook.readOrderBook(data);

        System.out.println(bidOrderBook.getAccountFlags().toString());

        Slab slab = bidOrderBook.getSlab();

        assertNotNull(slab);
//        assertEquals(67, slab.getBumpIndex());
//        assertEquals(28, slab.getFreeListLen());
//        assertEquals(22, slab.getFreeListHead());
//        assertEquals(20, slab.getLeafCount());

        slab.getSlabNodes().forEach(slabNode -> {
            if (slabNode instanceof SlabLeafNode) {
                //-6415612020026633454
                if (((SlabLeafNode)slabNode).getClientOrderId() == -6415612020026633454L) {
                    // found 3038.50      0.543320
                    System.out.println("FOUND");

                    SlabLeafNode slabLeafNode = (SlabLeafNode)slabNode;
                    long price = Utils.readInt64(slabLeafNode.getKey(), 0);

                    System.out.println("price = " + price);
                }
            }
        });
    }

    @Test
    public void testPriceDeserialization() {
        /* C:\apps\solanaj\orderbook3.dat (1/12/2021 8:55:59 AM)
   StartOffset(d): 00001277, EndOffset(d): 00001292, Length(d): 00000016 */

        byte[] rawData = {
                (byte)0xDB, (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };

        long price = Utils.readInt64(rawData, 0);
        BigInteger price2 = ByteUtils.readUint64(rawData, 0);
        BigInteger price3 = ByteUtils.readUint64Price(rawData, 0);
        long seqNum = Utils.readInt64(rawData, 8);

        System.out.println("Price = " + price + ", Price2 = " + price2 + ", Price3 = " + price3);
        System.out.println("seqNum = " + seqNum);


    }

    @Test
    public void orderBook3Test() {
        byte[] data = new byte[0];

        try {
            data = Files.readAllBytes(Paths.get("orderbook3.dat"));  // LQID/USDC
        } catch (IOException e) {
            e.printStackTrace();
        }

        OrderBook bidOrderBook = OrderBook.readOrderBook(data);
        System.out.println(bidOrderBook.getAccountFlags().toString());
        Slab slab = bidOrderBook.getSlab();

        assertNotNull(slab);

        /* C:\apps\solanaj\orderbook3.dat (1/12/2021 8:55:59 AM)
   StartOffset(d): 00001709, EndOffset(d): 00001724, Length(d): 00000016 */

        // this rawData = key bytes for a 477.080 quantity bid at 0.0510 cents

        byte[] rawData = {
                (byte)0xFC, (byte)0xFD, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0x33, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };


        slab.getSlabNodes().forEach(slabNode -> {
            if (slabNode instanceof SlabLeafNode) {
                SlabLeafNode slabLeafNode = (SlabLeafNode) slabNode;
                if (Arrays.equals(rawData, slabLeafNode.getKey())) {
                    System.out.println("Found the order");
                }
                System.out.println(slabNode);
                //System.out.println("Price = " + getPriceFromKey(slabLeafNode.getKey()));

            }
        });
    }

    /**
     * Returns a price long from a (price, seqNum) 128-bit key
     *
     * @param data
     * @return
     */
    private static long getPriceFromKey(byte[] data) {
        return ByteUtils.readUint64(data, 8).longValue();
    }

}
