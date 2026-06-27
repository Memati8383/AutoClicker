# AutoClicker

<p align="center">
  <a href="https://github.com/Memati8383/AutoClicker/releases/latest">
    <img src="https://img.shields.io/badge/s%C3%BCr%C3%BCm-v1.2-brightgreen" alt="Sürüm"/>
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/lisans-MIT-blue" alt="Lisans"/>
  </a>
  <a href="https://github.com/Memati8383/AutoClicker/releases">
    <img src="https://img.shields.io/badge/APK-indir-orange" alt="İndir"/>
  </a>
  <a href="https://github.com/Memati8383/AutoClicker/releases">
    <img src="https://img.shields.io/badge/API-24%2B-ff69b4" alt="API"/>
  </a>
</p>

Android için **floating overlay** üzerinden çalışan, **tek veya çoklu hedef** desteği sunan otomatik tıklama uygulaması.

---

## Özellikler

- 🎯 **Tek veya çoklu hedef** — istediğin kadar nokta belirle, sırayla tıklasın
- 🖱️ **Floating overlay** — tüm kontroller ekran üstünde, uygulamayı kapatmana gerek yok
- ⏱ **Özelleştirilebilir aralık** — milisaniye, saniye veya dakika cinsinden hız ayarı
- ⏹ **Durdurma koşulu** — süresiz, süre veya tıklama sayısına göre otomatik dur
- 🔄 **Hızlı aralık presets** — 0.1sn, 0.5sn, 1sn, 2sn, 5sn tek tıkla seç
- ⏳ **Kalan süre/sayı göstergesi** — çalışırken kalan süreyi veya tıklama sayısını overlay'de gör
- 🌙 **Karanlık/Aydınlık tema** — tercihine göre tema değiştir
- 🌐 **Türkçe / İngilizce** — dil desteği, tek tıkla değiştir
- 🤖 **Accessibility Service** — Android'in erişilebilirlik altyapısı ile çalışır
- 🎨 **Material 3 tasarım** — modern ve akıcı arayüz

## Kurulum

APK'yı [Releases](https://github.com/Memati8383/AutoClicker/releases) sayfasından indirip telefonuna yükleyebilirsin.

```
Ayarlar > Güvenlik > Bilinmeyen kaynaklardan yükle
```

## Kullanım

1. Uygulamayı aç
2. **Erişilebilirlik Servisi** iznini ver (Ayarlar > Erişilebilirlik > Auto Clicker)
3. **Overlay iznini** kabul et
4. Ana ekrandaki switch ile **floating paneli** aç
5. Paneldeki **⚙** butonundan tıklama hızı ve durdurma koşulunu ayarla
6. Hedefi sürükleyerek istediğin noktaya taşı
7. **▶** butonuna bas, otomatik tıklama başlasın!

## Gereksinimler

- Android 7.0+ (API 24+)
- Accessibility Service izni
- Overlay (diğer uygulamaların üstünde görüntüleme) izni

## Derleme

```bash
./gradlew assembleRelease
```

APK çıktısı: `app/build/outputs/apk/release/app-release.apk`

## Lisans

Bu proje MIT lisansı ile lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.
