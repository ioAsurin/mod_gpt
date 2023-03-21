package io.asurin.mod_gpt;


import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public final class Mod_gpt extends JavaPlugin {
    public static void main(String[] args) {

    }

    public String API_KEY,MODEL_ENDPOINT;
    private HttpClient httpClient;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        httpClient = HttpClient.newHttpClient();
        this.loadConfig();
        getCommand("gpt").setExecutor(this);
        getLogger().info("mod_gptがupしました");
            }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
       if (cmd.getName().equalsIgnoreCase("gpt")) {
           // gptコマンド が実行された時に実行
           Player player = (Player) sender;
           String message;
           if (args.length > 0) {
               message = String.join(" ", args);
           } else {
               sender.sendMessage("/gpt [comment]形式で使ってください");
               return true;
           }
           String response = askgpt(message);
           exec_build(player, response);
           //完了処理
           logparser(player,response,message);

           return true;
       }else {
           sender.sendMessage("だめだった！ごめん！");
       }
       return false;
   }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("mod_gptがdownしました");
    }

    private void loadConfig() {
        super.saveDefaultConfig();

        this.API_KEY = super.getConfig().getString("apiKey");
        this.MODEL_ENDPOINT = super.getConfig().getString("model_endpoint");
        getLogger().info("##DEBUG##APIキーの読み込み完了");
        getLogger().info("##DEBUG##APIキーは"+API_KEY);
        assert this.API_KEY != null;
        if (this.API_KEY.contains("your-api-key")) {
            getLogger().info("APIキーが読み込めなかった！");
        }
    }
    private void logparser(Player player, String response, String message){
        player.sendMessage(ChatColor.DARK_GRAY + "(たぶん）実行されました");
        String log_response = "##RESPONSE##\n" + response;
        String log_message = "##MESSAGE##\n" + message;
        getLogger().info(log_response);
        getLogger().info(log_message);
        getLogger().info("Logparser was done");
    }
    private String askgpt(String message) {
        try {
            UUID requestId = UUID.randomUUID();
            String prompt = "Minecraftで\"" + message + "\"という指示を実現するためのJavaコードを提供してください。\n" +
                    "コードは、Bukkitライブラリを使用し、プレイヤーが構造物のマテリアルとサイズを選択できるようにする必要があります。\n" +
                    "以下のルールに従ってコードを生成してください。\n" +
                    "\n" +
                    "- プレイヤーが選択したマテリアルとサイズで指定された構造物（例：小さな家、2x2の床など）を作成する\n" +
                    "- プレイヤーの現在の位置に基づいて構造物を作成する\n" +
                    "- 構造物が完成したら、プレイヤーを構造物の近くに移動させ、全体を見渡せる位置にする\n" +
                    "- コードは、必要なインポート文、イベントリスナー、および実行可能なメソッドを含めるべきです。\n";

            String requestBody = "{\"prompt\": \"" + prompt + "\", \"max_tokens\": 300, \"temperature\": 0.7, \"n\": 1, \"stop\": \"\\n\"}";
            getLogger().info(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODEL_ENDPOINT))
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Authorization", "Bearer " + API_KEY)
                    .setHeader("OpenAI-Request-Id", requestId.toString())
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            Bukkit.getLogger().info("API Response: " + response.body());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JSONObject jsonResponse = new JSONObject(response.body());
                String result = jsonResponse.getJSONArray("choices").getJSONObject(0).getString("text").trim();

                if (result.isEmpty()) {
                    Bukkit.getLogger().info("APIから適切なコードが生成されませんでした。もう一度お試しください。");
                    return "";
                } else {
                    return result;
                }
            } else {
                Bukkit.getLogger().warning("Error response from OpenAI API: " + response.statusCode());
                Bukkit.getLogger().info("Sorry, I could not generate a house structure at this time.");
                return "";
            }
        } catch (Exception ex) {
            Bukkit.getLogger().warning("Error generating response from OpenAI API: " + ex.getMessage());
            return "Sorry, an error occurred while generating the house structure.";
        }
    }

    private void exec_build(Player player, String response) {
        // レスポンスをJSONオブジェクトにパースする
        JSONObject jsonResponse = new JSONObject(response);

        // 必要なデータをJSONオブジェクトから抽出する
        JSONArray coordinates = jsonResponse.getJSONArray("coordinates");
        String blockType = jsonResponse.getString("blockType");
        JSONObject playerPosition = jsonResponse.getJSONObject("playerPosition");

        // 座標に基づいてブロックを配置する
        Material material = Material.matchMaterial(blockType);
        if (material == null) {
            player.sendMessage(ChatColor.RED + "Invalid block type: " + blockType);
            return;
        }

        World world = player.getWorld();
        for (int i = 0; i < coordinates.length(); i++) {
            JSONObject coordinate = coordinates.getJSONObject(i);
            int x = coordinate.getInt("x");
            int y = coordinate.getInt("y");
            int z = coordinate.getInt("z");

            world.getBlockAt(x, y, z).setType(material);
        }

        // プレイヤーの位置を更新する
        int playerX = playerPosition.getInt("x");
        int playerY = playerPosition.getInt("y");
        int playerZ = playerPosition.getInt("z");

        player.teleport(new Location(world, playerX, playerY, playerZ));
    }
}






