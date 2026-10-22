package app.heartwood.domain

/** A line in both languages. */
class Words(val en: String, val ru: String) {
    operator fun invoke(ru: Boolean) = if (ru) this.ru else en
}

fun w(en: String, ru: String) = Words(en, ru)

/** How a crown grows, for the engraving. */
enum class Form { BROAD, ROUND, OVAL, SLENDER, WEEPING, COLUMN, SMALL, PINE, SPRUCE }

/** The leaf drawn in the herbarium and the species key. */
enum class LeafShape { OAK, BIRCH, MAPLE, LINDEN, WILLOW, NEEDLES, SPRIG, FEATHER, HAND, OVAL, POPLAR }

/** What a rubbing of the bark shows. */
enum class BarkStyle { FURROWS, LENTICELS, SHALLOW, FINE, PLATES, SCALES, DIAMONDS, SMOOTH }

/** Something a tree does once a year that's worth a call: "I'm in blossom, come". */
class Yearly(val months: IntRange, val key: String, val call: Words)

/**
 * The trees one can befriend. [k] is the forester's growth factor: age ≈ diameter in inches × k.
 * [male] is the tree's grammatical gender in Russian (дуб — он, берёза — она), for the lines it says.
 */
enum class Species(
    val title: Words,
    val male: Boolean,
    val k: Double,
    val hint: Words,
    val character: Words,
    val form: Form,
    val leaf: LeafShape,
    val bark: BarkStyle,
    /** Autumn colours of the crown, ARGB. */
    val autumn: List<Long>,
    val evergreen: Boolean,
    val lines: List<Words>,
    val events: List<Yearly>,
    /** What the tree passes on to the others through the roots, said about it. */
    val news: List<Words>,
) {
    OAK(
        w("Oak", "Дуб"), true, 5.0,
        w("lobed leaves, acorns, deeply furrowed bark", "лист с лопастями, жёлуди, кора в глубоких бороздах"),
        w("unhurried, wise", "немногословный, мудрый"),
        Form.BROAD, LeafShape.OAK, BarkStyle.FURROWS, listOf(0xFF9A6B33, 0xFF8A6A35, 0xFFB3843F), false,
        listOf(
            w("I'm in no hurry. Don't you hurry either.", "Я не тороплюсь. И ты не торопись."),
            w("A strong wind last night. I stood. As usual.", "Ночью был сильный ветер. Я выстоял. Как обычно."),
            w("The bark is warmer on the south side. Put your palm there.", "С южной стороны кора теплее. Положи ладонь."),
            w("Jays hide my acorns and forget where. That's how oaks walk.", "Сойки прячут мои жёлуди и забывают где. Так дубы и ходят."),
            w("I've seen a lot of people. You I remember.", "Я видел много людей. Тебя я помню."),
        ),
        listOf(
            Yearly(9..10, "acorns", w("Acorns are falling. Come and take one — maybe it'll be an oak in two hundred years.", "Падают жёлуди. Приходи, возьми один — вдруг через двести лет он будет дубом.")),
            Yearly(5..5, "leaves", w("I'm the last to get my leaves. Everyone's green, and I'm only now. Come and see.", "Я одеваюсь последним. Все уже зелёные, а я только сейчас. Приходи посмотреть.")),
        ),
        listOf(w("the jays are hiding his acorns again.", "сойки опять прячут его жёлуди."), w("he stood through the night's wind and sends regards.", "он выстоял ночной ветер и шлёт привет."), w("acorns are falling on everybody's heads.", "жёлуди падают всем на головы.")),
    ),
    BIRCH(
        w("Birch", "Берёза"), false, 5.0,
        w("white bark with black marks, small toothed leaves", "белая кора с чёрными штрихами, мелкие зубчатые листья"),
        w("chatty, full of light", "болтливая, светлая"),
        Form.SLENDER, LeafShape.BIRCH, BarkStyle.LENTICELS, listOf(0xFFD9B13F, 0xFFE6C65C, 0xFFC9A136), false,
        listOf(
            w("At last! I've seen so much since you were here!", "Наконец-то! Я столько всего видела!"),
            w("A magpie sat on my third branch again, imagine.", "Сорока опять сидела на моей третьей ветке, представляешь?"),
            w("In spring my sap is sweet. That's a secret.", "Весной у меня сладкий сок. Это секрет."),
            w("My bark peels like paper. People used to write letters on it.", "Кора у меня как бумага. На ней раньше писали письма."),
            w("Wind's coming — I'll be the first to tell you.", "Ветер идёт — я первая тебе скажу."),
        ),
        listOf(
            Yearly(4..4, "sap", w("Spring is running up me as sap. Come and hear me wake up.", "Весна пошла по мне соком. Приходи — послушай, как я просыпаюсь.")),
            Yearly(9..10, "gold", w("I'm turning gold, a leaf at a time. Come before I've told the whole street.", "Я желтею по листочку. Приходи, пока не рассказала всей улице.")),
        ),
        listOf(w("a magpie has moved onto her third branch.", "на её третьей ветке поселилась сорока."), w("she's turning gold and can't stop talking about it.", "она желтеет и не может об этом молчать."), w("she heard the wind first and warns everyone.", "она первая услышала ветер и всех предупреждает.")),
    ),
    MAPLE(
        w("Maple", "Клён"), true, 4.5,
        w("palm-shaped leaves, helicopter seeds", "лист-ладонь, семена-вертолётики"),
        w("bright, impatient", "яркий, нетерпеливый"),
        Form.ROUND, LeafShape.MAPLE, BarkStyle.SHALLOW, listOf(0xFFC8452F, 0xFFD9682F, 0xFFE08A3A), false,
        listOf(
            w("My seeds are helicopters. Catch one.", "Мои семена — вертолётики. Лови."),
            w("In autumn I'm the loudest tree in the yard. In colour.", "Осенью я самый громкий во дворе. Цветом."),
            w("I can't wait for anything. Not even spring.", "Я ничего не умею ждать. Даже весну."),
            w("Put a leaf of mine in a book. It'll keep the autumn.", "Положи мой лист в книгу. Он сохранит осень."),
            w("My sap is sweet too. Don't tell the birch.", "Сок у меня тоже сладкий. Только берёзе не говори."),
        ),
        listOf(
            Yearly(10..10, "red", w("I'm all red. In a week I'll be bare — come and look.", "Я весь красный. Через неделю облечу — приходи посмотреть.")),
            Yearly(4..4, "flowers", w("I flower before my leaves, yellow-green. Nobody notices. You will.", "Я цвету раньше листьев, жёлто-зелёным. Никто не замечает. Ты заметишь.")),
        ),
        listOf(w("he's all red and very proud.", "он весь красный и очень гордый."), w("he's throwing helicopters at passers-by.", "он кидается вертолётиками в прохожих."), w("spring is late and he's had enough.", "весна опаздывает, и ему надоело ждать.")),
    ),
    LINDEN(
        w("Linden", "Липа"), false, 3.0,
        w("heart-shaped leaves, smells of honey in July", "лист-сердечко, в июле пахнет мёдом"),
        w("warm, smells of honey", "тёплая, медовая"),
        Form.OVAL, LeafShape.LINDEN, BarkStyle.FINE, listOf(0xFFD4A63A, 0xFFC09440, 0xFFB98F3C), false,
        listOf(
            w("In July I smell of honey. The bees know.", "В июле я пахну мёдом. Пчёлы знают."),
            w("Sit down. My shade is soft.", "Садись. Тень у меня мягкая."),
            w("My wood is soft too — people carve spoons from it.", "Древесина у меня тоже мягкая — из неё режут ложки."),
            w("My leaves are hearts. I didn't plan it.", "Листья у меня — сердечки. Я не нарочно."),
            w("Bees hum in me all day. You hum a little too.", "Пчёлы гудят во мне весь день. Ты тоже немного гудишь."),
        ),
        listOf(Yearly(7..7, "blossom", w("I'm in blossom. The whole street smells of honey — come and breathe.", "Я зацвела. Вся улица пахнет мёдом — приходи подышать."))),
        listOf(w("she's in blossom and the bees won't leave her.", "она цветёт, и пчёлы от неё не отходят."), w("she sends her shade and her regards.", "она шлёт тень и привет."), w("she feels a new ring growing.", "она чувствует, как растёт новое кольцо.")),
    ),
    PINE(
        w("Pine", "Сосна"), false, 3.5,
        w("long needles in pairs, orange bark up high", "длинная хвоя по две, рыжая кора вверху"),
        w("quiet, dependable", "молчаливая, надёжная"),
        Form.PINE, LeafShape.NEEDLES, BarkStyle.PLATES, listOf(0xFF4F6B3A, 0xFF5E7A44), true,
        listOf(
            w("…", "…"),
            w("I smell of resin. That's instead of words.", "Пахну смолой. Это вместо слов."),
            w("I'm green in winter too. You can count on me.", "Я зелёная и зимой. На меня можно положиться."),
            w("Listen. Up at the top I hum.", "Послушай. Наверху я шумлю."),
            w("My cones open when it's dry and close for rain. I'm a barometer.", "Шишки у меня раскрываются в сухую погоду и закрываются к дождю. Я барометр."),
        ),
        listOf(
            Yearly(1..2, "green", w("Everyone is bare and I'm green. Come and see something green.", "Все голые, а я зелёная. Приходи посмотреть на зелёное.")),
            Yearly(5..6, "pollen", w("I'm dusting yellow pollen on everything. Sorry. Come anyway.", "Я сыплю жёлтую пыльцу на всё вокруг. Прости. Всё равно приходи.")),
        ),
        listOf(w("she says nothing, but it's a warm nothing.", "она молчит, но тепло молчит."), w("she smells of resin today.", "она сегодня пахнет смолой."), w("she's green as ever.", "она зелёная, как всегда.")),
    ),
    SPRUCE(
        w("Spruce", "Ель"), false, 4.5,
        w("short sharp needles all round, a cone-shaped crown", "короткие колючие иголки, крона конусом"),
        w("strict, keeps secrets", "строгая, хранит секреты"),
        Form.SPRUCE, LeafShape.SPRIG, BarkStyle.SCALES, listOf(0xFF3F5A32, 0xFF4F6B3A), true,
        listOf(
            w("Under my lower branches it's dry even in the rain. Remember that.", "Под нижними ветками сухо даже в дождь. Запомни."),
            w("Squirrels keep their stores in me. I don't tell.", "Белки держат у меня запасы. Я не выдаю."),
            w("Snow suits me. I know.", "Мне идёт снег. Я знаю."),
            w("I'm strict. But I'm here.", "Я строгая. Но я рядом."),
            w("My needles live seven years. I'm patient.", "Мои иголки живут лет семь. Я терпеливая."),
        ),
        listOf(
            Yearly(12..12, "snow", w("Snow has settled on my paws. Come and see me in white.", "Снег лёг мне на лапы. Приходи посмотреть на меня в белом.")),
            Yearly(6..6, "shoots", w("I have new soft tips, bright green. Touch them — they don't prick yet.", "У меня новые мягкие кончики, ярко-зелёные. Потрогай — они ещё не колются.")),
        ),
        listOf(w("she's keeping a squirrel's secret.", "она хранит беличий секрет."), w("she says snow suits her.", "она говорит, что ей идёт снег."), w("she's strict as usual, which means all is well.", "она строгая, как обычно, — значит, всё хорошо.")),
    ),
    WILLOW(
        w("Willow", "Ива"), false, 2.5,
        w("long narrow leaves, branches down to the ground", "узкие длинные листья, ветви до земли"),
        w("thoughtful, by the water", "задумчивая, у воды"),
        Form.WEEPING, LeafShape.WILLOW, BarkStyle.DIAMONDS, listOf(0xFFB8B05A, 0xFFA7A24E, 0xFFC2B86A), false,
        listOf(
            w("I look into the water. I'm there too.", "Я смотрю в воду. Там тоже я."),
            w("I'm not sad. I'm thoughtful.", "Мне не грустно. Мне задумчиво."),
            w("Push a twig of mine into the ground and it'll grow. I'm stubborn.", "Воткни мою веточку в землю — вырастет. Я упрямая."),
            w("Walk in among my branches. It's a room in there.", "Зайди внутрь моих ветвей. Там комната."),
            w("My bark is bitter. It once cured headaches.", "Кора у меня горькая. Когда-то ею лечили головную боль."),
        ),
        listOf(Yearly(3..4, "catkins", w("My catkins are out, soft and grey. This is the very first spring.", "Распушились мои котики — серые, мягкие. Это самая первая весна."))),
        listOf(w("she's looking into the water again.", "она опять смотрит в воду."), w("she's let her branches down to the ground.", "она опустила ветви до самой земли."), w("she's thoughtful, not sad — she asks to make that clear.", "ей задумчиво, а не грустно — просила уточнить.")),
    ),
    ROWAN(
        w("Rowan", "Рябина"), false, 4.5,
        w("feather leaves of many leaflets, red berry clusters", "перистый лист из многих листочков, красные гроздья"),
        w("modest, flares up in autumn", "скромная, вспыхивает осенью"),
        Form.SMALL, LeafShape.FEATHER, BarkStyle.SMOOTH, listOf(0xFFC2482E, 0xFFD87A34, 0xFFB5402C), false,
        listOf(
            w("My berries are for the birds. Leave them some.", "Ягоды у меня для птиц. Оставь им."),
            w("After the frost my berries get sweeter. Like some people.", "После мороза ягоды слаще. Как некоторые люди."),
            w("People used to plant me by the house for luck.", "Меня сажали у дома на счастье."),
            w("I'm small. But in autumn the whole yard looks at me.", "Я небольшая. Но осенью на меня смотрит весь двор."),
            w("Waxwings will come in winter. Come too.", "Зимой прилетят свиристели. Приходи и ты."),
        ),
        listOf(
            Yearly(8..9, "berries", w("My berries are red. The thrushes know already — come before they do.", "Ягоды покраснели. Дрозды уже знают — приходи раньше них.")),
            Yearly(1..2, "waxwings", w("Waxwings are eating my berries. Come and look, they're beautiful.", "Свиристели едят мои ягоды. Приходи посмотреть, они красивые.")),
        ),
        listOf(w("she has red berries and a crowd of thrushes.", "у неё красные ягоды и толпа дроздов."), w("the waxwings are on their way.", "свиристели уже летят."), w("she's small but bright today.", "она небольшая, но сегодня яркая.")),
    ),
    CHESTNUT(
        w("Horse chestnut", "Каштан"), true, 8.0,
        w("a big hand of five to seven leaflets, shiny conkers", "большой лист-пятерня, блестящие каштаны"),
        w("generous, a bit of a dandy", "щедрый, немного франт"),
        Form.BROAD, LeafShape.HAND, BarkStyle.SHALLOW, listOf(0xFFB9783A, 0xFFA8692F, 0xFFC99247), false,
        listOf(
            w("My conkers shine as if polished. Take one for your pocket.", "Мои каштаны блестят, как начищенные. Возьми один в карман."),
            w("In May I light candles. White ones.", "В мае я зажигаю свечи. Белые."),
            w("My leaves are hands. I wave.", "Листья у меня — ладони. Я машу."),
            w("I'm a bit of a dandy. Don't tell anyone.", "Я немного франт. Никому не говори."),
            w("My shade is the widest in the street.", "Тень у меня самая широкая на улице."),
        ),
        listOf(
            Yearly(5..5, "candles", w("My candles are lit, white, all over me. Come before they go out.", "Зажглись мои свечи — белые, по всей кроне. Приходи, пока не погасли.")),
            Yearly(9..9, "conkers", w("Conkers are falling, shiny ones. Take one for your pocket.", "Падают каштаны, блестящие. Возьми один в карман.")),
        ),
        listOf(w("he has lit his candles.", "он зажёг свои свечи."), w("he's dropping conkers on cars.", "он роняет каштаны на машины."), w("he waves at everyone with his leaves.", "он машет всем листьями.")),
    ),
    APPLE(
        w("Apple", "Яблоня"), false, 4.5,
        w("oval leaves, a crooked trunk, blossom and apples", "овальные листья, кривой ствол, цвет и яблоки"),
        w("kind, a little crooked", "добрая, немного кривая"),
        Form.SMALL, LeafShape.OVAL, BarkStyle.SCALES, listOf(0xFFC9A03A, 0xFFB68A35, 0xFF9C8A3A), false,
        listOf(
            w("I'm crooked. That's character.", "Я кривая. Это характер."),
            w("My apples are sour. Honestly sour.", "Яблоки у меня кислые. Честно кислые."),
            w("Someone planted me long ago. I'm still waiting for them.", "Меня кто-то посадил давно. Я до сих пор его жду."),
            w("Bees love me in May. You can love me any month.", "В мае меня любят пчёлы. А ты можешь — в любой месяц."),
            w("There's a hollow in me just right for a starling.", "Во мне есть дупло как раз для скворца."),
        ),
        listOf(
            Yearly(5..5, "blossom", w("I'm in blossom, white and pink. It lasts a week — come.", "Я цвету, бело-розовым. Это на неделю — приходи.")),
            Yearly(8..9, "apples", w("Apples are falling. Pick one up — it's yours.", "Падают яблоки. Подними одно — оно твоё.")),
        ),
        listOf(w("she's in blossom and very happy.", "она цветёт и очень счастлива."), w("she dropped an apple on a cat.", "она уронила яблоко на кота."), w("the starling is back in her hollow.", "в её дупло вернулся скворец.")),
    ),
    POPLAR(
        w("Poplar", "Тополь"), true, 2.0,
        w("tall and narrow, trembling leaves, fluff in June", "высокий и узкий, листья дрожат, пух в июне"),
        w("tall, talks with the wind", "высокий, говорит с ветром"),
        Form.COLUMN, LeafShape.POPLAR, BarkStyle.DIAMONDS, listOf(0xFFD6B84A, 0xFFC4A73F), false,
        listOf(
            w("I grow fast. Faster than anyone here.", "Я расту быстро. Быстрее всех здесь."),
            w("My leaves talk to the wind all the time. Listen.", "Мои листья всё время говорят с ветром. Послушай."),
            w("The fluff in June — that's me. Sorry.", "Пух в июне — это я. Извини."),
            w("From up here I can see the river.", "Отсюда сверху я вижу реку."),
            w("I'm tall, but I'm not proud.", "Я высокий, но не гордый."),
        ),
        listOf(
            Yearly(6..6, "fluff", w("My fluff is flying all over town. Come and stand in the June snow.", "Мой пух летит по всему городу. Приходи постоять в июньском снегу.")),
            Yearly(4..5, "leaves", w("My new leaves are sticky and smell of spring.", "Новые листья у меня клейкие и пахнут весной.")),
        ),
        listOf(w("he can see the river today.", "ему сегодня видно реку."), w("he's sending fluff everywhere.", "он рассылает пух во все стороны."), w("he's talking with the wind and says hello.", "он говорит с ветром и передаёт привет.")),
    ),
    ASH(
        w("Ash", "Ясень"), true, 4.0,
        w("feather leaves, smooth grey bark, bunches of winged keys", "перистые листья, гладкая серая кора, гроздья крылаток"),
        w("light, open, late", "лёгкий, открытый, поздний"),
        Form.OVAL, LeafShape.FEATHER, BarkStyle.FINE, listOf(0xFFB7A44E, 0xFF9FA046, 0xFFC4B05A), false,
        listOf(
            w("I put my leaves on late and take them off early. I like being light.", "Листья я надеваю поздно и снимаю рано. Люблю лёгкость."),
            w("Old tales say the whole world grows on an ash.", "В старых сказках на ясене держится весь мир."),
            w("My keys hang in bunches all winter.", "Мои крылатки-ключики висят гроздьями всю зиму."),
            w("Light passes through me. Stand in it.", "Через меня проходит свет. Постой в нём."),
            w("Bows and oars are made of me. I'm not bragging.", "Из меня делают луки и вёсла. Я не хвастаюсь."),
        ),
        listOf(
            Yearly(10..10, "bare", w("I dropped my leaves all at once, still green. Ashes can do that. Come and see the keys.", "Я сбросил листья разом, зелёными. Ясени так умеют. Приходи посмотреть на ключики.")),
            Yearly(5..5, "late", w("Leaves, at last. I'm always the last in the street.", "Наконец-то листья. Я всегда последний на улице.")),
        ),
        listOf(w("he dropped his leaves all at once.", "он сбросил листья разом."), w("the world is still holding on, he says.", "мир всё ещё держится, говорит он."), w("he lets the light through and sends you some.", "он пропускает свет и передаёт тебе немного.")),
    );

    /** Age from the girth: the circumference to an inch diameter, times the growth factor. */
    fun age(girthCm: Int): Int = Math.round(girthCm / Math.PI / 2.54 * k).toInt()

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: OAK
    }
}
