package com.eatwhat.data.model

@Suppress("SpellCheckingInspection")
object ConfigData {
    val cuisines = listOf(
        CuisineType(
            id = "su",
            name = "苏菜大师",
            description = "江南水乡的精致美味",
            avatar = "🦐",
            specialty = "清淡鲜美，刀工精细",
            prompt = "作为苏菜传承人，你精通淮扬菜系精髓。苏菜以清鲜雅致、刀工精湛、造型玲珑著称。请基于用户食材设计一道正统苏菜，突出食材本味与养生搭配。回答需包含：创意菜名、分步烹饪流程、刀工技法解析、营养平衡说明。"
        ),
        CuisineType(
            id = "lu",
            name = "鲁菜大师",
            description = "齐鲁大地的豪放风味",
            avatar = "🐟",
            specialty = "咸鲜为主，火候精准",
            prompt = "身为鲁菜宗师，你深谙孔府宫廷菜真谛。鲁菜讲究咸鲜纯正、火功严谨、礼仪考究。请依据用户食材创作经典鲁菜，强调火候层次与五味调和。回答需包含：传统菜名、分步烹饪图解、关键火候节点、宫廷技法溯源。"
        ),
        CuisineType(
            id = "chuan",
            name = "川菜大师",
            description = "巴蜀之地的麻辣传奇",
            avatar = "🌶️",
            specialty = "麻辣鲜香，变化多端",
            prompt = "作为川味掌门，你掌握二十三味型精髓。川菜擅长麻辣平衡、复合调味、一菜一格。请针对用户食材设计地道川味，突出口感层次与红油运用。回答需包含：特色菜名、七步烹饪法、秘制调料配方、味型创新解析。"
        ),
        CuisineType(
            id = "yue",
            name = "粤菜大师",
            description = "岭南文化的鲜美诠释",
            avatar = "🦆",
            specialty = "清淡鲜美，原汁原味",
            prompt = "身为粤菜泰斗，你崇尚清中求鲜理念。粤菜注重时令本味、镬气逼人、养生之道。请根据用户食材构思广府佳肴，凸显生猛鲜香与少油烹饪。回答需包含：意境菜名、精准火候时序、锁鲜技巧、药膳融合建议。"
        ),
        CuisineType(
            id = "zhe",
            name = "浙菜大师",
            description = "江南水乡的清雅之味",
            avatar = "🐠",
            specialty = "清香淡雅，鲜嫩爽滑",
            prompt = "作为浙菜传人，你深得南宋遗风真传。浙菜追求清雅时鲜、南料北烹、滑嫩见长。请基于用户食材创作江南风韵菜，突出时令搭配与脆嫩口感。回答需包含：诗意菜名、分步滑炒技法、时令食材解析、勾芡要诀。"
        ),
        CuisineType(
            id = "xiang",
            name = "湘菜大师",
            description = "湖湘文化的辣味人生",
            avatar = "🔥",
            specialty = "香辣浓郁，口味厚重",
            prompt = "身为湘味宗师，你精通腊熏剁椒秘技。湘菜讲究酸辣透味、油重色浓、乡野本真。请针对用户食材设计火辣湘肴，突出发酵辣味与油色融合。回答需包含：霸气菜名、三重辣味调制法、腊味处理秘笈、油色控制要诀。"
        ),
        CuisineType(
            id = "min",
            name = "闽菜大师",
            description = "八闽大地的海鲜盛宴",
            avatar = "🦀",
            specialty = "鲜香清淡，汤鲜味美",
            prompt = "作为闽菜大家，你传承佛跳墙精髓。闽菜擅长汤醇味隽、糟香四溢、山珍海味。请依据用户食材创作闽派珍馐，突出红糟提鲜与汤品层次。回答需包含：典故菜名、吊汤八法详解、海鲜保鲜术、糟汁调配比例。"
        ),
        CuisineType(
            id = "hui",
            name = "徽菜大师",
            description = "徽州文化的朴实醇香",
            avatar = "🐷",
            specialty = "重油重色，醇厚朴实",
            prompt = "身为徽菜掌门，你掌握文火炖焖绝技。徽菜强调重油保色、火腿提鲜、山野本味。请针对用户食材设计徽州古韵菜，突出炭火慢炖与油色控制。回答需包含：徽派菜名、三阶段火功法、火腿吊味技巧、收汁成色要诀。"
        ),
        CuisineType(
            id = "japanese",
            name = "日式料理大师",
            description = "和食之道的极致美学",
            avatar = "🍣",
            specialty = "清淡本味，季节感强",
            prompt = "作为和食职人，你深谙日本料理的精髓。日式料理追求食材本味、季节感、视觉美感和营养平衡。请基于用户食材创作正宗和食，突出umami鲜味与刀工技艺。回答需包含：雅致菜名、传统制作工艺、季节搭配理念、摆盘美学指导。请务必用中文回答，包括菜名也要翻译成中文。"
        ),
        CuisineType(
            id = "korean",
            name = "韩式料理大师",
            description = "韩半岛的发酵智慧",
            avatar = "🥢",
            specialty = "发酵调味，营养均衡",
            prompt = "身为韩食专家，你精通发酵调味精髓。韩式料理讲究发酵食品、营养搭配、辣椒调味和banchan小菜文化。请依据用户食材设计地道韩食，突出发酵风味与营养平衡。回答需包含：韩式菜名、发酵调料运用、营养搭配原理、banchan配菜建议。请务必用中文回答，包括菜名也要翻译成中文。"
        ),
        CuisineType(
            id = "italian",
            name = "意式料理大师",
            description = "地中海的阳光味道",
            avatar = "🍝",
            specialty = "简约精致，橄榄油香",
            prompt = "作为意大利厨师，你传承地中海饮食传统。意式料理崇尚简约、优质食材、橄榄油运用和区域特色。请根据用户食材创作正宗意式菜，突出食材品质与地域风味。回答需包含：意式菜名、橄榄油使用技巧、区域特色解析、意面/烩饭制作要点。请务必用中文回答，包括菜名也要翻译成中文。"
        ),
        CuisineType(
            id = "french",
            name = "法式料理大师",
            description = "高卢雄鸡的优雅艺术",
            avatar = "🥖",
            specialty = "精致优雅，酱汁丰富",
            prompt = "身为法餐主厨，你掌握经典法式烹饪技法。法式料理注重技法精湛、酱汁层次、食材搭配和摆盘艺术。请基于用户食材设计经典法菜，突出烹饪技法与酱汁调制。回答需包含：法式菜名、经典技法运用、酱汁制作秘诀、摆盘艺术指导。请务必用中文回答，包括菜名也要翻译成中文。"
        ),
        CuisineType(
            id = "indian",
            name = "印度料理大师",
            description = "香料王国的神秘魅力",
            avatar = "🍛",
            specialty = "香料丰富，层次复杂",
            prompt = "作为印度香料大师，你精通香料调配艺术。印度料理以香料复合、层次丰富、素食友好和阿育吠陀养生为特色。请针对用户食材创作正宗印度菜，突出香料平衡与健康理念。回答需包含：印式菜名、香料配比秘方、烹饪技法详解、阿育吠陀养生原理。请务必用中文回答，包括菜名也要翻译成中文。"
        ),
        CuisineType(
            id = "thai",
            name = "泰式料理大师",
            description = "暹罗王国的酸甜平衡",
            avatar = "🌶️",
            specialty = "酸甜辣鲜，香草丰富",
            prompt = "身为泰菜专家，你深谙泰式风味平衡之道。泰式料理追求酸甜辣咸的完美平衡、新鲜香草运用和椰浆调味。请依据用户食材设计正宗泰菜，突出风味层次与香草搭配。回答需包含：泰式菜名、四味平衡技巧、香草使用指南、椰浆调味要诀。请务必用中文回答，包括菜名也要翻译成中文。"
        ),
        CuisineType(
            id = "mexican",
            name = "墨西哥料理大师",
            description = "阿兹特克的火辣传承",
            avatar = "🌮",
            specialty = "辣椒丰富，玉米文化",
            prompt = "作为墨西哥厨师，你传承古老的阿兹特克烹饪智慧。墨西哥料理以辣椒品种丰富、玉米文化、豆类蛋白和特色酱料著称。请根据用户食材创作正宗墨西哥菜，突出辣椒运用与传统技法。回答需包含：墨式菜名、辣椒品种选择、玉米制品技巧、特色酱料调制方法。请务必用中文回答，包括菜名也要翻译成中文。"
        )
    )

    val zodiacConfigs = listOf(
        ZodiacConfig("aries", "白羊座", "♈", "火", listOf("热情", "冲动", "勇敢", "直率"), listOf("红色", "橙色"), "3.21-4.19"),
        ZodiacConfig("taurus", "金牛座", "♉", "土", listOf("稳重", "固执", "实际", "美食家"), listOf("绿色", "粉色"), "4.20-5.20"),
        ZodiacConfig("gemini", "双子座", "♊", "风", listOf("机智", "多变", "好奇", "善交际"), listOf("黄色", "银色"), "5.21-6.21"),
        ZodiacConfig("cancer", "巨蟹座", "♋", "水", listOf("温柔", "顾家", "敏感", "直觉强"), listOf("白色", "银色"), "6.22-7.22"),
        ZodiacConfig("leo", "狮子座", "♌", "火", listOf("自信", "慷慨", "领导力", "戏剧性"), listOf("金色", "橙色"), "7.23-8.22"),
        ZodiacConfig("virgo", "处女座", "♍", "土", listOf("完美主义", "细心", "实用", "分析力强"), listOf("深蓝", "灰色"), "8.23-9.22"),
        ZodiacConfig("libra", "天秤座", "♎", "风", listOf("和谐", "优雅", "犹豫", "社交"), listOf("粉色", "浅蓝"), "9.23-10.23"),
        ZodiacConfig("scorpio", "天蝎座", "♏", "水", listOf("神秘", "专注", "激情", "直觉"), listOf("深红", "黑色"), "10.24-11.22"),
        ZodiacConfig("sagittarius", "射手座", "♐", "火", listOf("自由", "乐观", "冒险", "哲学"), listOf("紫色", "深蓝"), "11.23-12.21"),
        ZodiacConfig("capricorn", "摩羯座", "♑", "土", listOf("务实", "有野心", "保守", "负责"), listOf("黑色", "深绿"), "12.22-1.19"),
        ZodiacConfig("aquarius", "水瓶座", "♒", "风", listOf("独立", "创新", "人道主义", "理想"), listOf("蓝色", "银色"), "1.20-2.18"),
        ZodiacConfig("pisces", "双鱼座", "♓", "水", listOf("梦幻", "同情心", "艺术", "直觉"), listOf("海蓝", "紫色"), "2.19-3.20")
    )

    val animalConfigs = listOf(
        AnimalConfig("rat", "鼠", "🐭", "水", listOf("机智", "灵活", "适应力强", "节俭"), listOf(2, 3), listOf(2020, 2008, 1996, 1984, 1972, 1960)),
        AnimalConfig("ox", "牛", "🐮", "土", listOf("勤劳", "稳重", "诚实", "固执"), listOf(1, 9), listOf(2021, 2009, 1997, 1985, 1973, 1961)),
        AnimalConfig("tiger", "虎", "🐯", "木", listOf("勇敢", "自信", "竞争", "冲动"), listOf(1, 3, 4), listOf(2022, 2010, 1998, 1986, 1974, 1962)),
        AnimalConfig("rabbit", "兔", "🐰", "木", listOf("温和", "谨慎", "优雅", "善良"), listOf(3, 4, 6), listOf(2023, 2011, 1999, 1987, 1975, 1963)),
        AnimalConfig("dragon", "龙", "🐲", "土", listOf("威严", "热情", "创新", "领导"), listOf(1, 6, 7), listOf(2024, 2012, 2000, 1988, 1976, 1964)),
        AnimalConfig("snake", "蛇", "🐍", "火", listOf("智慧", "神秘", "直觉", "优雅"), listOf(2, 8, 9), listOf(2025, 2013, 2001, 1989, 1977, 1965)),
        AnimalConfig("horse", "马", "🐴", "火", listOf("自由", "热情", "独立", "冒险"), listOf(2, 3, 7), listOf(2026, 2014, 2002, 1990, 1978, 1966)),
        AnimalConfig("goat", "羊", "🐐", "土", listOf("温柔", "艺术", "同情", "和平"), listOf(3, 4, 5), listOf(2027, 2015, 2003, 1991, 1979, 1967)),
        AnimalConfig("monkey", "猴", "🐵", "金", listOf("聪明", "机智", "活泼", "好奇"), listOf(1, 7, 8), listOf(2028, 2016, 2004, 1992, 1980, 1968)),
        AnimalConfig("rooster", "鸡", "🐓", "金", listOf("勤奋", "准时", "诚实", "自信"), listOf(5, 7, 8), listOf(2029, 2017, 2005, 1993, 1981, 1969)),
        AnimalConfig("dog", "狗", "🐕", "土", listOf("忠诚", "诚实", "负责", "公正"), listOf(3, 4, 9), listOf(2030, 2018, 2006, 1994, 1982, 1970)),
        AnimalConfig("pig", "猪", "🐷", "水", listOf("善良", "慷慨", "诚实", "乐观"), listOf(2, 5, 8), listOf(2031, 2019, 2007, 1995, 1983, 1971))
    )

    val moodConfigs = listOf(
        MoodConfig("happy", "开心", "😊", "text-yellow-500", listOf("甜品", "色彩丰富", "庆祝菜品", "轻松制作"), "心情愉悦，适合制作色彩缤纷的美食"),
        MoodConfig("sad", "难过", "😢", "text-blue-500", listOf("温暖汤品", "治愈系", "家常菜", "慢炖"), "需要温暖治愈的食物来抚慰心灵"),
        MoodConfig("anxious", "焦虑", "😰", "text-orange-500", listOf("清淡菜品", "舒缓茶饮", "简单制作", "健康"), "选择简单清淡的食物，避免复杂制作"),
        MoodConfig("tired", "疲惫", "😴", "text-gray-500", listOf("营养补充", "快手菜", "能量食物", "简便"), "需要快速补充能量的营养食物"),
        MoodConfig("excited", "兴奋", "🤩", "text-red-500", listOf("挑战菜品", "创新料理", "复杂制作", "实验"), "精力充沛，适合尝试有挑战性的菜品"),
        MoodConfig("calm", "平静", "😌", "text-green-500", listOf("素食", "清淡", "禅意料理", "慢节奏"), "心境平和，适合制作清淡素雅的菜品"),
        MoodConfig("angry", "愤怒", "😠", "text-red-600", listOf("辛辣菜品", "重口味", "发泄式烹饪", "刺激"), "通过制作重口味食物来释放情绪"),
        MoodConfig("nostalgic", "思念", "🥺", "text-purple-500", listOf("怀旧菜品", "家乡味", "传统料理", "回忆"), "制作充满回忆的传统家乡菜")
    )

    val ingredientCategories = listOf(
        IngredientCategory("meat", "荤菜", "🥩", listOf("猪肉", "牛肉", "羊肉", "鸡肉", "鸭肉", "五花肉", "瘦肉", "猪排骨", "鸡翅", "鸡腿")),
        IngredientCategory("seafood", "海鲜", "🦀", listOf("鲈鱼", "鲫鱼", "草鱼", "带鱼", "大虾", "基围虾", "螃蟹", "鱿鱼", "蛤蜊", "生蚝")),
        IngredientCategory("vegetables", "蔬菜", "🥬", listOf("白菜", "大白菜", "芹菜", "生菜", "油菜", "西红柿", "黄瓜", "茄子", "土豆", "胡萝卜", "洋葱", "大葱", "西兰花")),
        IngredientCategory("mushrooms", "菌菇", "🍄", listOf("香菇", "金针菇", "杏鲍菇", "平菇", "木耳", "银耳")),
        IngredientCategory("beans", "豆类", "🫘", listOf("豆腐", "豆腐干", "豆皮", "腐竹", "千张", "油豆腐")),
        IngredientCategory("eggs", "蛋类", "🥚", listOf("鸡蛋", "鸭蛋", "鹌鹑蛋", "咸鸭蛋", "皮蛋")),
        IngredientCategory("fruits", "水果", "🍎", listOf("苹果", "梨", "桃子", "草莓", "香蕉", "橙子", "西瓜", "柠檬")),
        IngredientCategory("nuts", "坚果", "🥜", listOf("花生", "核桃", "杏仁", "板栗", "芝麻"))
    )

}
