package com.example.wayang_detection.data.repository

import com.example.wayang_detection.R
import com.example.wayang_detection.data.model.WayangCategory
import com.example.wayang_detection.data.model.WayangCharacter

/**
 * Repository containing all 16 Balinese Wayang Kulit characters.
 * Class IDs correspond to YOLOv11 model output indices (alphabetical order).
 *
 * IMPORTANT: Replace imageResId with actual drawable resources after
 * placing wayang images in res/drawable/. Naming convention:
 *   wayang_acintya.jpg, wayang_arjuna.jpg, wayang_bhatara_siwa.jpg, etc.
 */
object WayangRepository {

    private val characters = listOf(
        // ── ID 0: Acintya ──
        WayangCharacter(
            id = "acintya",
            name = "Acintya",
            aliases = listOf("Sang Hyang Widhi Wasa", "Sang Hyang Tunggal"),
            category = WayangCategory.DEWA,
            group = "Dewa Tertinggi",
            traits = listOf("Maha Kuasa", "Transenden", "Abstrak"),
            description = "Acintya adalah konsep Tuhan Yang Maha Esa dalam kepercayaan Hindu Bali. " +
                    "Beliau merupakan sumber dari segala yang ada, pencipta alam semesta, dan " +
                    "kekuatan tertinggi yang melampaui pemahaman manusia. Dalam pewayangan Bali, " +
                    "Acintya digambarkan sebagai sosok yang berada di puncak hierarki ketuhanan, " +
                    "mewakili kesatuan dari semua dewa dan kekuatan kosmis.",
            philosophy = "Acintya melambangkan konsep ketuhanan yang melampaui bentuk dan nama. " +
                    "Filosofinya mengajarkan bahwa Tuhan tidak dapat dijangkau oleh pikiran " +
                    "manusia biasa (acintya = tak terpikirkan). Keberadaan-Nya hanya dapat " +
                    "dirasakan melalui manifestasi-Nya dalam alam semesta.",
            visualTraits = listOf(
                "Posisi di atas gunungan (kayon)",
                "Dikelilingi api dan ornamen kosmis",
                "Figur meditatif dengan mahkota tinggi",
                "Warna emas dominan"
            ),
            imageResId = R.drawable.wayang_acintya,
            modelClassId = 0
        ),

        // ── ID 1: Arjuna ──
        WayangCharacter(
            id = "arjuna",
            name = "Arjuna",
            aliases = listOf("Janaka", "Parta", "Permadi"),
            category = WayangCategory.PROTAGONIS,
            group = "Pandawa Lima",
            traits = listOf("Tampan", "Cekatan", "Ahli Panah"),
            description = "Arjuna adalah putra ketiga Pandu dan Kunti, sekaligus ksatria pemanah " +
                    "terhebat dalam epos Mahabharata. Ia dikenal sebagai sosok yang tampan, " +
                    "halus budi pekertinya, dan sangat mahir dalam seni memanah. Arjuna " +
                    "memiliki senjata pusaka Pasupati pemberian Bhatara Siwa dan busur Gandewa. " +
                    "Ia adalah murid terbaik Resi Drona dalam ilmu perang.",
            philosophy = "Arjuna melambangkan kehalusan budi dan ketajaman pikiran. Tubuhnya yang " +
                    "ramping namun kuat menunjukkan bahwa kekuatan sejati tidak selalu datang " +
                    "dari fisik yang besar, melainkan dari keterampilan, konsentrasi, dan " +
                    "kebijaksanaan. Dialog Bhagavad Gita antara Arjuna dan Krisna merupakan " +
                    "inti filosofi Hindu tentang dharma dan karma.",
            visualTraits = listOf(
                "Tubuh ramping dan halus",
                "Mata sipit (liyep) menandakan kehalusan",
                "Mahkota (gelung) khas ksatria",
                "Membawa busur panah",
                "Warna kulit halus/terang"
            ),
            imageResId = R.drawable.wayang_arjuna,
            modelClassId = 1
        ),

        // ── ID 2: Bhatara Siwa ──
        WayangCharacter(
            id = "bhatara_siwa",
            name = "Bhatara Siwa",
            aliases = listOf("Sang Hyang Siwa", "Mahadewa", "Rudra"),
            category = WayangCategory.DEWA,
            group = "Tri Murti",
            traits = listOf("Pelebur", "Bijaksana", "Maha Kuasa"),
            description = "Bhatara Siwa adalah salah satu dari Tri Murti (trinitas Hindu) yang " +
                    "berperan sebagai dewa pelebur atau penghancur alam semesta. Dalam kosmologi " +
                    "Bali, Siwa menempati posisi tertinggi di antara Tri Murti (Brahma-Wisnu-Siwa). " +
                    "Beliau bersemayam di Gunung Mahameru dan merupakan sumber kebijaksanaan " +
                    "serta kekuatan spiritual tertinggi.",
            philosophy = "Siwa sebagai pelebur mengajarkan bahwa kehancuran adalah bagian dari " +
                    "siklus penciptaan. Tanpa penghancuran yang lama, tidak ada ruang untuk " +
                    "yang baru. Filosofi ini sejalan dengan konsep Rwa Bhineda (dualitas) " +
                    "dalam budaya Bali — keseimbangan antara penciptaan dan peleburan.",
            visualTraits = listOf(
                "Mahkota tinggi bersusun (jatamakuta)",
                "Wajah tenang dan bijaksana",
                "Mata tiga (tri netra) di dahi",
                "Atribut bulan sabit",
                "Duduk di atas padmasana"
            ),
            imageResId = R.drawable.wayang_bhatara_siwa,
            modelClassId = 2
        ),

        // ── ID 3: Bima ──
        WayangCharacter(
            id = "bima",
            name = "Bima",
            aliases = listOf("Werkudara", "Bratasena", "Bayusuta"),
            category = WayangCategory.PROTAGONIS,
            group = "Pandawa Lima",
            traits = listOf("Kuat", "Pemberani", "Setia"),
            description = "Bima adalah putra kedua Pandu dan Kunti, serta ksatria terkuat di antara " +
                    "Pandawa Lima. Ia memiliki kekuatan luar biasa yang diperoleh dari ayah " +
                    "dewinya, Dewa Bayu (dewa angin). Bima dikenal dengan keberaniannya yang " +
                    "tanpa batas, kesetiaannya kepada keluarga, serta sifatnya yang blak-blakan " +
                    "dan tidak kenal kompromi dalam menegakkan kebenaran.",
            philosophy = "Wajah hitam Bima melambangkan ketenangan batin dan kejujuran tanpa " +
                    "kepura-puraan. Tubuh besarnya menunjukkan kekuatan fisik dan mental yang " +
                    "kokoh. Kuku Pancanaka (kuku yang menjadi senjata) mengajarkan bahwa " +
                    "kekuatan sejati sudah ada dalam diri sendiri. Bima juga merupakan " +
                    "simbol pencarian kebenaran (Dewa Ruci).",
            visualTraits = listOf(
                "Tubuh besar dan kekar",
                "Wajah hitam/gelap",
                "Mata membelalak (thelengan)",
                "Kuku Pancanaka di jari",
                "Rambut tergerai (gelung minangkara)",
                "Kain poleng (hitam-putih)"
            ),
            imageResId = R.drawable.wayang_bima,
            modelClassId = 3
        ),

        // ── ID 4: Delem ──
        WayangCharacter(
            id = "delem",
            name = "Delem",
            aliases = listOf("Melem"),
            category = WayangCategory.PUNAKAWAN,
            group = "Punakawan Kiri",
            traits = listOf("Sombong", "Lucu", "Penakut"),
            description = "Delem adalah salah satu punakawan (abdi/pelawak) dari pihak kiri " +
                    "(pihak Kurawa/antagonis) dalam pewayangan Bali. Ia selalu tampil bersama " +
                    "Sangut sebagai pasangan punakawan kiri. Delem dikenal dengan sifatnya yang " +
                    "sombong, suka pamer, namun sebenarnya penakut. Ia sering menjadi sumber " +
                    "humor dalam pertunjukan wayang melalui dialog-dialog kocaknya.",
            philosophy = "Delem merepresentasikan sifat manusia yang penuh kesombongan namun " +
                    "rapuh di dalam. Ia mengajarkan bahwa kesombongan tanpa isi hanya akan " +
                    "mengundang tawa. Melalui karakternya, dalang menyampaikan kritik sosial " +
                    "dengan cara yang menghibur.",
            visualTraits = listOf(
                "Tubuh pendek dan gemuk",
                "Wajah merah/gelap",
                "Perut buncit",
                "Mata besar melotot",
                "Gigi tonggos",
                "Gerak-gerik berlebihan"
            ),
            imageResId = R.drawable.wayang_delem,
            modelClassId = 4
        ),

        // ── ID 5: Durga ──
        WayangCharacter(
            id = "durga",
            name = "Durga",
            aliases = listOf("Dewi Durga", "Rangda", "Bathari Durga"),
            category = WayangCategory.ANTAGONIS,
            group = "Raksasa",
            traits = listOf("Mengerikan", "Sakti", "Gelap"),
            description = "Durga dalam pewayangan Bali adalah sosok dewi yang berubah menjadi " +
                    "ratu kegelapan. Awalnya ia adalah Dewi Uma, istri Bhatara Siwa, yang " +
                    "dikutuk menjadi Durga karena suatu pelanggaran. Ia kemudian menguasai " +
                    "alam kegelapan (setra/kuburan) dan menjadi pemimpin para leak dan makhluk " +
                    "jahat. Durga sering dikaitkan dengan sosok Rangda dalam mitologi Bali.",
            philosophy = "Durga mengajarkan bahwa bahkan yang suci pun bisa terjerumus ke " +
                    "kegelapan jika melanggar dharma. Namun ia juga mewakili kekuatan feminin " +
                    "yang dahsyat (shakti). Transformasinya menjadi simbol bahwa perubahan " +
                    "adalah hal yang tak terhindarkan, dan penebusan selalu mungkin.",
            visualTraits = listOf(
                "Wajah menyeramkan dengan taring",
                "Rambut terurai panjang",
                "Mata besar menakutkan",
                "Lidah menjulur",
                "Kuku panjang seperti cakar",
                "Ornamen tengkorak"
            ),
            imageResId = R.drawable.wayang_durga,
            modelClassId = 5
        ),

        // ── ID 6: Duryodana ──
        WayangCharacter(
            id = "duryodana",
            name = "Duryodana",
            aliases = listOf("Suyodana", "Kurupati"),
            category = WayangCategory.ANTAGONIS,
            group = "Kurawa",
            traits = listOf("Ambisius", "Iri Hati", "Keras Kepala"),
            description = "Duryodana adalah putra sulung Drestarastra dan pemimpin seratus " +
                    "bersaudara Kurawa. Ia merupakan antagonis utama dalam epos Mahabharata. " +
                    "Duryodana dikenal karena ambisinya yang besar untuk menguasai kerajaan " +
                    "Hastinapura, rasa irinya terhadap Pandawa, dan keengganannya untuk " +
                    "berbagi bahkan sejengkal tanah pun kepada sepupu-sepupunya.",
            philosophy = "Duryodana melambangkan bahaya dari keserakahan dan iri hati yang " +
                    "tidak terkendali. Meski ia seorang ksatria yang pemberani dan kuat, " +
                    "sifat tamaknya membawanya pada kehancuran. Ia mengajarkan bahwa " +
                    "kekuatan tanpa dharma (kebenaran) akan berujung pada kebinasaan.",
            visualTraits = listOf(
                "Wajah lebar dan arogan",
                "Mahkota raja (makuta)",
                "Mata tajam dan angkuh",
                "Tubuh besar dan tegap",
                "Busana kerajaan mewah",
                "Sering membawa gada"
            ),
            imageResId = R.drawable.wayang_duryodana,
            modelClassId = 6
        ),

        // ── ID 7: Krisna ──
        WayangCharacter(
            id = "krisna",
            name = "Krisna",
            aliases = listOf("Narayana", "Kesawa", "Govinda"),
            category = WayangCategory.PROTAGONIS,
            group = "Keluarga Pandawa",
            traits = listOf("Bijaksana", "Diplomatik", "Sakti"),
            description = "Krisna adalah titisan (avatar) Dewa Wisnu yang turun ke bumi untuk " +
                    "menegakkan dharma. Ia berperan sebagai penasihat, diplomat, dan pelindung " +
                    "utama Pandawa Lima. Krisna dikenal karena kebijaksanaannya yang tiada tara, " +
                    "kemampuan diplomasinya, serta kekuatan ilahiahnya. Dialog antara Krisna " +
                    "dan Arjuna dalam Bhagavad Gita menjadi salah satu ajaran spiritual " +
                    "terpenting dalam Hindu.",
            philosophy = "Krisna adalah guru spiritual tertinggi yang mengajarkan tentang " +
                    "dharma (kewajiban), karma (perbuatan), dan bhakti (pengabdian). " +
                    "Kebijaksanaannya dalam Bhagavad Gita mengajarkan bahwa seseorang " +
                    "harus menjalankan kewajibannya tanpa terikat pada hasilnya (nishkama karma).",
            visualTraits = listOf(
                "Wajah halus dan teduh",
                "Kulit gelap/biru kehitaman",
                "Mahkota indah dengan bulu merak",
                "Membawa cakra (senjata lempar)",
                "Busana kerajaan anggun",
                "Sering digambarkan tersenyum"
            ),
            imageResId = R.drawable.wayang_krisna,
            modelClassId = 7
        ),

        // ── ID 8: Kunti ──
        WayangCharacter(
            id = "kunti",
            name = "Kunti",
            aliases = listOf("Dewi Kunti", "Prita", "Dewi Madrim"),
            category = WayangCategory.PROTAGONIS,
            group = "Keluarga Pandawa",
            traits = listOf("Bijaksana", "Sabar", "Tabah"),
            description = "Kunti adalah istri Prabu Pandu dan ibu dari tiga Pandawa tertua: " +
                    "Yudhistira, Bima, dan Arjuna. Ia dikenal sebagai sosok ibu yang sangat " +
                    "bijaksana, sabar, tabah, dan penuh kasih sayang. Kunti memiliki kemampuan " +
                    "memanggil para dewa berkat mantra dari Resi Durwasa. Ia menjadi tiang " +
                    "kekuatan moral bagi putra-putranya sepanjang kisah Mahabharata.",
            philosophy = "Kunti melambangkan kekuatan seorang ibu yang tak terukur. " +
                    "Kesabarannya dalam menghadapi penderitaan mengajarkan bahwa ketabahan " +
                    "dan kasih sayang adalah kekuatan terbesar. Ia menjadi simbol " +
                    "pengorbanan ibu demi kebahagiaan dan kebenaran anak-anaknya.",
            visualTraits = listOf(
                "Wajah lembut dan teduh",
                "Sanggul rapi (gelung)",
                "Busana putri istana yang anggun",
                "Ornamen emas sederhana",
                "Postur tubuh anggun dan berwibawa"
            ),
            imageResId = R.drawable.wayang_kunti,
            modelClassId = 8
        ),

        // ── ID 9: Madri ──
        WayangCharacter(
            id = "madri",
            name = "Madri",
            aliases = listOf("Dewi Madri"),
            category = WayangCategory.PROTAGONIS,
            group = "Keluarga Pandawa",
            traits = listOf("Cantik", "Setia", "Pengorbanan"),
            description = "Madri adalah istri kedua Prabu Pandu dan ibu dari si kembar " +
                    "Nakula dan Sadewa. Ia adalah putri Raja Madra yang terkenal akan " +
                    "kecantikannya. Madri dikenal karena kesetiaannya yang luar biasa " +
                    "kepada suaminya. Saat Pandu meninggal, Madri memilih untuk menyusul " +
                    "suaminya dengan melakukan sati (upacara pengorbanan diri), menyerahkan " +
                    "pengasuhan putranya kepada Kunti.",
            philosophy = "Madri melambangkan kesetiaan dan pengorbanan tanpa batas. " +
                    "Kecantikannya bukan hanya lahiriah tetapi juga batiniah. " +
                    "Keputusannya untuk meninggalkan dunia bersama suaminya " +
                    "mengajarkan tentang ikatan cinta dan dharma seorang istri " +
                    "dalam tradisi Hindu.",
            visualTraits = listOf(
                "Wajah cantik dan anggun",
                "Sanggul putri kerajaan",
                "Busana mewah khas putri Madra",
                "Ornamen perhiasan halus",
                "Ekspresi lembut dan penuh kasih"
            ),
            imageResId = R.drawable.wayang_madri,
            modelClassId = 9
        ),

        // ── ID 10: Merdah ──
        WayangCharacter(
            id = "merdah",
            name = "Merdah",
            aliases = listOf("Wredah"),
            category = WayangCategory.PUNAKAWAN,
            group = "Punakawan Kanan",
            traits = listOf("Cerdas", "Lincah", "Nakal"),
            description = "Merdah adalah punakawan (abdi sekaligus pelawak) dari pihak kanan " +
                    "(pihak Pandawa/protagonis) dalam pewayangan Bali. Ia adalah putra " +
                    "dari Tualen dan selalu mendampingi ayahnya. Merdah dikenal karena " +
                    "kecerdasannya, kelincahannya, dan sifat nakalnya yang menghibur. " +
                    "Ia sering melontarkan humor-humor segar dan menjadi penyambung " +
                    "pesan dalang kepada penonton.",
            philosophy = "Merdah merepresentasikan generasi muda yang cerdas namun " +
                    "masih perlu bimbingan. Kelincahannya mengajarkan pentingnya " +
                    "adaptasi, sementara kenakalan ringannya mengingatkan bahwa " +
                    "kehidupan perlu diselingi kegembiraan. Bersama Tualen, ia " +
                    "menjadi jembatan kebijaksanaan antara dunia wayang dan penonton.",
            visualTraits = listOf(
                "Tubuh kecil dan lincah",
                "Wajah ceria dan ekspresif",
                "Mata bersinar cerdas",
                "Gerakan gesit dan enerjik",
                "Sering membawa alat musik atau properti"
            ),
            imageResId = R.drawable.wayang_merdah,
            modelClassId = 10
        ),

        // ── ID 11: Nakula-Sadewa ──
        WayangCharacter(
            id = "nakula_sadewa",
            name = "Nakula-Sadewa",
            aliases = listOf("Si Kembar", "Putra Madri"),
            category = WayangCategory.PROTAGONIS,
            group = "Pandawa Lima",
            traits = listOf("Setia", "Rendah Hati", "Kompak"),
            description = "Nakula dan Sadewa adalah putra kembar Pandu dan Madri, serta " +
                    "dua anggota termuda Pandawa Lima. Nakula dikenal sebagai pria " +
                    "tertampan di antara Pandawa yang ahli dalam ilmu pengobatan kuda, " +
                    "sementara Sadewa ahli dalam ilmu bintang (astronomi) dan spiritual. " +
                    "Keduanya sangat setia dan selalu kompak, menjadi simbol persaudaraan " +
                    "yang tak terpisahkan.",
            philosophy = "Si kembar Nakula-Sadewa melambangkan keseimbangan dan kesatuan. " +
                    "Kerendahan hati mereka mengajarkan bahwa tidak semua kekuatan harus " +
                    "ditunjukkan. Mereka mengingatkan bahwa dalam setiap kelompok, peran " +
                    "pendukung sama pentingnya dengan peran utama. Kesetiaan mereka " +
                    "kepada kakak-kakak menjadi contoh bhakti dalam persaudaraan.",
            visualTraits = listOf(
                "Wajah tampan dan halus",
                "Tubuh sedang, proporsional",
                "Busana ksatria yang rapi",
                "Sering digambarkan berdampingan",
                "Ornamen mahkota sederhana"
            ),
            imageResId = R.drawable.wayang_nakula_sadewa,
            modelClassId = 11
        ),

        // ── ID 12: Sangut ──
        WayangCharacter(
            id = "sangut",
            name = "Sangut",
            aliases = listOf("Nang Sangut"),
            category = WayangCategory.PUNAKAWAN,
            group = "Punakawan Kiri",
            traits = listOf("Bijaksana", "Humoris", "Filosofis"),
            description = "Sangut adalah punakawan dari pihak kiri (pihak Kurawa) yang " +
                    "selalu berpasangan dengan Delem. Meski berada di pihak 'antagonis', " +
                    "Sangut justru dikenal sebagai sosok yang bijaksana dan filosofis. " +
                    "Ia sering menjadi pengimbang bagi kelakuan konyol Delem. " +
                    "Dalam pertunjukan wayang, Sangut berperan penting sebagai " +
                    "penyampai pesan-pesan moral dan kritik sosial dalang.",
            philosophy = "Sangut mengajarkan bahwa kebijaksanaan bisa ditemukan di mana saja, " +
                    "bahkan di pihak yang dianggap 'salah'. Ia mengingatkan bahwa kebaikan " +
                    "tidak ditentukan oleh kelompok, tetapi oleh pilihan individu. " +
                    "Humornya yang cerdas membuktikan bahwa tawa dan kebijaksanaan " +
                    "bisa berjalan berdampingan.",
            visualTraits = listOf(
                "Tubuh lebih kecil dari Delem",
                "Wajah lucu namun tenang",
                "Ekspresi bijaksana di balik humor",
                "Gerakan lebih tenang dan terkontrol",
                "Sering membawa properti kecil"
            ),
            imageResId = R.drawable.wayang_sangut,
            modelClassId = 12
        ),

        // ── ID 13: Sengkuni ──
        WayangCharacter(
            id = "sengkuni",
            name = "Sengkuni",
            aliases = listOf("Sakuni", "Arya Sengkuni", "Trigantalpati"),
            category = WayangCategory.ANTAGONIS,
            group = "Kurawa",
            traits = listOf("Licik", "Manipulatif", "Cerdik"),
            description = "Sengkuni adalah paman sekaligus penasihat utama Duryodana dan " +
                    "para Kurawa. Ia dikenal sebagai dalang di balik berbagai tipu muslihat " +
                    "yang merugikan Pandawa, termasuk permainan dadu curang yang menyebabkan " +
                    "Pandawa diasingkan selama 13 tahun. Sengkuni adalah simbol kelicikan " +
                    "dan manipulasi dalam pewayangan.",
            philosophy = "Sengkuni melambangkan bahaya dari kecerdasan yang digunakan " +
                    "untuk kejahatan. Ia mengajarkan bahwa kepintaran tanpa moral adalah " +
                    "senjata yang menghancurkan. Kejatuhannya membuktikan bahwa kelicikan " +
                    "pada akhirnya akan terungkap dan mendapat hukumannya. " +
                    "Ia menjadi peringatan tentang pentingnya integritas.",
            visualTraits = listOf(
                "Wajah licik dengan senyum miring",
                "Mata sipit dan tajam",
                "Tubuh kurus dan bungkuk",
                "Jari-jari panjang dan kurus",
                "Busana pejabat istana",
                "Sering menunjukkan gestur berbisik"
            ),
            imageResId = R.drawable.wayang_sengkuni,
            modelClassId = 13
        ),

        // ── ID 14: Tualen ──
        WayangCharacter(
            id = "tualen",
            name = "Tualen",
            aliases = listOf("Nang Tualen", "Punta"),
            category = WayangCategory.PUNAKAWAN,
            group = "Punakawan Kanan",
            traits = listOf("Bijaksana", "Setia", "Pengasih"),
            description = "Tualen adalah punakawan paling senior dan bijaksana dari pihak " +
                    "kanan (pihak Pandawa) dalam pewayangan Bali. Ia adalah ayah dari " +
                    "Merdah dan setia mendampingi para Pandawa. Tualen dianggap sebagai " +
                    "penjelmaan Sang Hyang Ciwa (Siwa) yang turun ke dunia untuk " +
                    "memberikan nasihat dan kebijaksanaan kepada manusia melalui humor.",
            philosophy = "Tualen adalah cerminan kebijaksanaan tertinggi yang hadir dalam " +
                    "bentuk paling sederhana. Ia mengajarkan bahwa ilmu sejati tidak " +
                    "memerlukan penampilan mewah. Kesetiaannya kepada Pandawa melambangkan " +
                    "pengabdian tanpa pamrih. Dalam tradisi Bali, Tualen dihormati sebagai " +
                    "penghubung antara dunia manusia dan dewa.",
            visualTraits = listOf(
                "Tubuh pendek dan sederhana",
                "Wajah tua dan bijaksana",
                "Mata kecil yang teduh",
                "Perut sedikit buncit",
                "Pakaian sederhana khas rakyat",
                "Ekspresi tenang dan sabar"
            ),
            imageResId = R.drawable.wayang_tualen,
            modelClassId = 14
        ),

        // ── ID 15: Yudhistira ──
        WayangCharacter(
            id = "yudhistira",
            name = "Yudhistira",
            aliases = listOf("Dharmaputra", "Puntadewa", "Ajatasatru"),
            category = WayangCategory.PROTAGONIS,
            group = "Pandawa Lima",
            traits = listOf("Adil", "Jujur", "Sabar"),
            description = "Yudhistira adalah putra sulung Pandu dan Kunti, serta pemimpin " +
                    "para Pandawa Lima. Ia dikenal sebagai Raja Dharma (raja kebenaran) " +
                    "karena kejujurannya yang absolut — ia tidak pernah berbohong sepanjang " +
                    "hidupnya (kecuali satu kali dalam perang Bharatayuddha). Yudhistira " +
                    "adalah simbol keadilan, kesabaran, dan kepemimpinan yang berdasarkan " +
                    "dharma (kebenaran).",
            philosophy = "Yudhistira melambangkan ideal seorang pemimpin yang memerintah " +
                    "dengan dharma. Kejujurannya mengajarkan bahwa kebenaran adalah " +
                    "senjata paling kuat. Kesabarannya yang luar biasa — bahkan di tengah " +
                    "pengasingan dan penderitaan — menunjukkan bahwa pemimpin sejati " +
                    "mengutamakan ketenangan dan keadilan di atas segalanya.",
            visualTraits = listOf(
                "Wajah tenang dan berwibawa",
                "Tubuh sedang, tegap",
                "Mahkota raja (ketu/makuta)",
                "Mata lembut dan teduh",
                "Busana raja yang sederhana namun bermartabat",
                "Postur tubuh tegak dan stabil"
            ),
            imageResId = R.drawable.wayang_yudhistira,
            modelClassId = 15
        )
    )

    /** Get all 16 characters */
    fun getAll(): List<WayangCharacter> = characters

    /** Get character by ID string */
    fun getById(id: String): WayangCharacter? = characters.find { it.id == id }

    /** Get character by YOLO model class ID */
    fun getByClassId(classId: Int): WayangCharacter? = characters.find { it.modelClassId == classId }

    /** Get characters filtered by category */
    fun getByCategory(category: WayangCategory): List<WayangCharacter> =
        characters.filter { it.category == category }

    /** Search characters by name or alias */
    fun search(query: String): List<WayangCharacter> {
        if (query.isBlank()) return characters
        val lowerQuery = query.lowercase()
        return characters.filter { char ->
            char.name.lowercase().contains(lowerQuery) ||
                    char.aliases.any { it.lowercase().contains(lowerQuery) } ||
                    char.group.lowercase().contains(lowerQuery)
        }
    }
}
