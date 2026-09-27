# UniRemote AI

Wi-Fi ağındaki hemen her akıllı TV'yi (markalı/markasız, DLNA/UPnP destekleyen
her cihaz dahil) bulup uzaktan kumanda etmeyi hedefleyen bir Android
(Kotlin + Jetpack Compose) proje iskeleti.

## Neyi gerçekten yapıyor

| Protokol | Durum | Not |
|---|---|---|
| SSDP tarama | ✅ Çalışan gerçek kod | `discovery/SsdpDiscovery.kt` — 239.255.255.250:1900'e M-SEARCH gönderir, yanıt veren her cihazı listeler. **Markasız TV'lerin çoğu buraya düşer.** |
| DLNA/UPnP kontrol | ✅ Çalışan gerçek kod | `protocols/DlnaController.kt` — ses, oynat/duraklat/durdur, URL cast (SOAP/AVTransport+RenderingControl). |
| Roku (ECP) | ✅ Çalışan gerçek kod | `protocols/RokuController.kt` — resmi, kimlik doğrulamasız HTTP API, port 8060. |
| LG WebOS (SSAP) | ✅ Gerçek protokol, TV onayı gerektirir | `protocols/LgWebOsController.kt` — ilk bağlantıda TV ekranında "İzin ver?" istemi çıkar, kullanıcı onaylamadan çalışmaz (LG'nin kendi güvenlik katmanı). |
| Samsung Tizen | ✅ Gerçek protokol, TV onayı gerektirir | `protocols/SamsungTizenController.kt` — aynı şekilde TV'de eşleştirme onayı ister. |
| ADB (markasız Android kutular) | ✅ Gerçek protokol, cihaz sahibinin "Kablosuz hata ayıklama"yı açması gerekir | `protocols/AdbController.kt` — `dadb` kütüphanesi ile saf Kotlin ADB istemcisi, `adb` binary'sine ihtiyaç duymaz. |

`MainActivity.kt` şu an Scan → Remote akışını **DLNA ve Roku için uçtan uca**
bağlıyor. LG/Samsung/ADB controller'ları tam işlevsel ama pairing adımını UI'a
bağlama işi (TV'de onay isteyen bir dialog göstermek) yapılmadı — her
controller'ın `connect()`/`connectAndPair()` fonksiyonundaki yorum satırları
bunu nasıl yapacağını anlatıyor.

## Önemli sınır (bug değil, tasarım)

LG, Samsung ve ADB için **cihaz sahibinin TV üzerinde fiziksel olarak onay
vermesi şart.** Bu, üreticilerin kendi güvenlik mekanizması; uygulamanın
bunu aşması mümkün değil ve olmamalı. Yani "hiç tanınmayan, kimsenin
haberi olmayan bir TV'ye bağlan" değil, "kendi TV'ni/kutunu bir kere
onaylayıp sonra rahatça kumanda et" senaryosu bu.

## Kurulum

1. Android Studio'da projeyi aç (Koala/Ladybug veya üstü önerilir).
2. `app/build.gradle.kts` içindeki `dadb` ve diğer kütüphane sürümlerini
   Maven Central'dan en güncel haliyle teyit et — bu ortamda internet
   erişimi olmadığı için sürüm numaraları doğrulanamadı.
3. Telefon ve TV **aynı Wi-Fi ağında** olmalı.
4. Çalıştır → uygulama açılır açılmaz otomatik SSDP taraması başlar.
5. `mipmap` ikon dosyaları eklenmedi; Android Studio'nun "New > Image Asset"
   sihirbazıyla birkaç saniyede eklenebilir (derleme için gerekli).

## Sırada ne var (genişletme önerileri)

- Ekran yansıtma (Cast) için DLNA `castUrl()` zaten var; telefonun kendi
  ekranını/fotoğrafını yayınlamak için basit bir yerel HTTP sunucusu
  (ör. NanoHTTPD) ekleyip o URL'i `castUrl()`'e vermek yeterli.
- LG/Samsung `client-key`/`token`'ları `DataStore` veya `SharedPreferences`
  ile kalıcı hale getirilmeli (şu an bellekte, uygulama kapanınca gider).
- Touchpad → LG/Samsung için gerçek imleç hareketi: WebOS'ta
  `ssap://com.webos.service.networkinput/getPointerInputSocket` ile ayrı bir
  soket açılıp oradan `move`/`click` gönderilir; Tizen'de `KEY_UP/DOWN/...`
  ile adım adım simüle edilir (gerçek imleç API'si yok).
