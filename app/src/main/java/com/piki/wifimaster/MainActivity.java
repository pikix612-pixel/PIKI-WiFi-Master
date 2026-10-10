package com.piki.wifimaster;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.DhcpInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout content;
    private WifiManager wifiManager;
    private TextView infoText;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        buildScreen();
        showWifiInfo();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(12, 18, 32));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 28, 24, 28);
        scroll.addView(content);

        TextView title = new TextView(this);
        title.setText("PIKI WiFi Master");
        title.setTextColor(Color.rgb(70, 210, 255));
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 8, 0, 6);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("UNIVERSAL NETWORK TOOLKIT");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 0, 0, 22);
        content.addView(subtitle);

        infoText = new TextView(this);
        infoText.setTextColor(Color.WHITE);
        infoText.setTextSize(15);
        infoText.setPadding(16, 18, 16, 18);
        infoText.setBackgroundColor(Color.rgb(25, 35, 53));
        content.addView(infoText);

        addButton("⟳  REFRESH INFO WI-FI", v -> showWifiInfo());
        addButton("▣  BUKA ADMIN ROUTER", v -> openRouter());
        addButton("◉  PANDUAN WDS / REPEATER", v -> showGuide());
        addButton("ⓘ  TENTANG APLIKASI", v -> showAbout());

        TextView note = new TextView(this);
        note.setText("\nGunakan hanya pada jaringan yang kamu miliki atau diizinkan untuk dikelola.");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 18, 0, 12);
        content.addView(note);

        setContentView(scroll);
    }

    private void addButton(String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setOnClickListener(action);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 12, 0, 0);
        content.addView(button, params);
    }

    private String ipString(int ip) {
        return String.format(Locale.US, "%d.%d.%d.%d",
                ip & 255, (ip >> 8) & 255,
                (ip >> 16) & 255, (ip >> 24) & 255);
    }

    private void showWifiInfo() {
        StringBuilder out = new StringBuilder();
        out.append("STATUS JARINGAN\n\n");

        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities caps = network == null || cm == null
                ? null : cm.getNetworkCapabilities(network);

        boolean connectedWifi = caps != null &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);

        out.append("Wi-Fi: ")
                .append(connectedWifi ? "Tersambung" : "Tidak terdeteksi")
                .append("\n");

        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            out.append("Radio Wi-Fi: mati atau tidak tersedia\n");
            infoText.setText(out.toString());
            return;
        }

        try {
            WifiInfo info = wifiManager.getConnectionInfo();

            if (info != null && info.getNetworkId() != -1) {
                String ssid = info.getSSID();
                if (ssid != null) ssid = ssid.replace("\"", "");
                out.append("Nama jaringan: ")
                        .append(ssid == null ? "Tidak diketahui" : ssid)
                        .append("\n");

                out.append("IP HP: ")
                        .append(ipString(info.getIpAddress()))
                        .append("\n");

                int rssi = info.getRssi();
                out.append("Sinyal: ")
                        .append(rssi <= -100 ? "Tidak diketahui" : rssi + " dBm")
                        .append("\n");

                if (rssi >= -55) out.append("Kualitas sinyal: Sangat kuat\n");
                else if (rssi >= -67) out.append("Kualitas sinyal: Baik\n");
                else if (rssi >= -75) out.append("Kualitas sinyal: Sedang\n");
                else out.append("Kualitas sinyal: Lemah\n");

                out.append("Frekuensi: ");
                int freq = info.getFrequency();
                out.append(freq > 0 ? freq + " MHz" : "Tidak diketahui")
                        .append("\n");
            } else {
                out.append("Detail Wi-Fi: belum tersambung\n");
            }

            DhcpInfo dhcp = wifiManager.getDhcpInfo();
            if (dhcp != null && dhcp.gateway != 0) {
                out.append("Gateway/router: ")
                        .append(ipString(dhcp.gateway)).append("\n");
            } else {
                out.append("Gateway/router: tidak diketahui\n");
            }

            out.append("\nCatatan: nama jaringan dapat disamarkan Android jika izin lokasi atau akses Wi-Fi tidak tersedia.");
        } catch (SecurityException e) {
            out.append("\nAndroid membatasi akses informasi Wi-Fi. Periksa izin aplikasi.");
        }

        infoText.setText(out.toString());
    }

    private void openRouter() {
        String address = null;
        try {
            DhcpInfo dhcp = wifiManager == null ? null : wifiManager.getDhcpInfo();
            if (dhcp != null && dhcp.gateway != 0) {
                address = ipString(dhcp.gateway);
            }
        } catch (SecurityException ignored) {}

        if (address == null) {
            address = "192.168.1.1";
        }

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://" + address));
        try {
            startActivity(intent);
        } catch (Exception e) {
            infoText.setText("Tidak dapat membuka browser. Coba alamat router: "
                    + address);
        }
    }

    private void showGuide() {
        infoText.setText(
                "PANDUAN WDS / REPEATER\n\n" +
                "1. Sambungkan HP ke jaringan router yang akan dikonfigurasi.\n\n" +
                "2. Tekan BUKA ADMIN ROUTER dan masuk dengan kredensial milikmu.\n\n" +
                "3. Cari menu Wireless, WDS, Repeater, Range Extender, atau Operation Mode. Nama menu berbeda menurut merek.\n\n" +
                "4. Pastikan router mendukung mode tersebut dan cocokkan band serta keamanan Wi-Fi sumber.\n\n" +
                "5. Pilih jaringan sumber dan masukkan kata sandi yang benar.\n\n" +
                "6. Atur alamat LAN agar tidak bentrok dengan router utama, lalu simpan sesuai petunjuk perangkat.\n\n" +
                "PENTING: Jangan ubah DHCP atau alamat LAN tanpa memahami konfigurasi. Catat pengaturan lama agar bisa dipulihkan. WDS tidak didukung oleh semua router.");
    }

    private void showAbout() {
        infoText.setText(
                "PIKI WiFi Master Universal\n\n" +
                "Versi 1.0\n\n" +
                "Alat bantu untuk memeriksa informasi jaringan Wi-Fi dan membuka halaman administrasi router.\n\n" +
                "Aplikasi ini tidak membobol kata sandi, tidak melewati keamanan router, dan tidak menjamin kompatibilitas semua perangkat.");
    }
}
