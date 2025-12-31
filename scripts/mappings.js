// Bilingual Category Names
const CATEGORY_MAP = {
    // English Key          // Display Name (English | Arabic)
    'premium': '🏆 Premium | المميزة',
    'sports': '⚽ Sports | رياضة',
    'movies': '🎬 Movies | أفلام',
    'series': '📺 Series | مسلسلات',
    'comedy': '😂 Comedy | كوميديا',
    'kids': '👶 Kids | أطفال',
    'animation': '👶 Kids | أطفال', // Merge Animation into Kids for simplicity or keep separate? Plan said Kids.
    'documentary': '🧠 Documentary | وثائقيات',
    'education': '🧠 Documentary | وثائقيات', // Merge
    'religious': '🕋 Religious | إسلاميات',
    'news': '📰 News | أخبار',
    'family': '🏡 Family | العائلة',
    'lifestyle': '🧘 Lifestyle | نمط الحياة',
    'general': '🌍 General | منوعات',
    'auto': '🚗 Auto | سيارات',
    'business': '💼 Business | أعمال',
    'music': '🎵 Music | موسيقى',
    'classic': '🕰️ Classic | كلاسيكيات'
};

// Country Code to Name Mapping
const COUNTRY_MAP = {
    'sa': '🇸🇦 Saudi Arabia | السعودية',
    'ae': '🇦🇪 UAE | الإمارات',
    'eg': '🇪🇬 Egypt | مصر',
    'kw': '🇰🇼 Kuwait | الكويت',
    'qa': '🇶🇦 Qatar | قطر',
    'bh': '🇧🇭 Bahrain | البحرين',
    'om': '🇴🇲 Oman | عمان',
    'iq': '🇮🇶 Iraq | العراق',
    'jo': '🇯🇴 Jordan | الأردن',
    'lb': '🇱🇧 Lebanon | لبنان',
    'ps': '🇵🇸 Palestine | فلسطين',
    'sy': '🇸🇾 Syria | سوريا',
    'ye': '🇾🇪 Yemen | اليمن',
    'sd': '🇸🇩 Sudan | السودان',
    'ly': '🇱🇾 Libya | ليبيا',
    'dz': '🇩🇿 Algeria | الجزائر',
    'ma': '🇲🇦 Morocco | المغرب',
    'tn': '🇹🇳 Tunisia | تونس'
};

// Common Channel Name Replacements (Regex supported if needed, but simple string replacement is safer/faster for now)
// Key: Part of the name to match (case insensitive), Value: Replacement
const CHANNEL_NAME_MAP = {
    'MBC 1': 'MBC 1 | إم بي سي 1',
    'MBC 2': 'MBC 2 | إم بي سي 2',
    'MBC 3': 'MBC 3 | إم بي سي 3',
    'MBC 4': 'MBC 4 | إم بي سي 4',
    'MBC 5': 'MBC 5 | إم بي سي 5',
    'MBC Action': 'MBC Action | إم بي سي أكشن',
    'MBC Max': 'MBC Max | إم بي سي ماكس',
    'MBC Drama': 'MBC Drama | إم بي سي دراما',
    'MBC Bollywood': 'MBC Bollywood | إم بي سي بوليوود',
    'MBC Masr': 'MBC Masr | إم بي سي مصر',
    'MBC Iraq': 'MBC Iraq | إم بي سي العراق',
    'Al Jazeera': 'Al Jazeera | الجزيرة',
    'Al Jazeera Documentary': 'Al Jazeera Documentary | الجزيرة الوثائقية',
    'Al Arabiya': 'Al Arabiya | العربية',
    'Al Hadath': 'Al Hadath | الحدث',
    'Sky News Arabia': 'Sky News Arabia | سكاي نيوز عربية',
    'CNN International': 'CNN International',
    'BBC World News': 'BBC World News',
    'Osn': 'OSN',
    'Rotana Cinema': 'Rotana Cinema | روتانا سينما',
    'Rotana Comedy': 'Rotana Comedy | روتانا كوميديا',
    'Rotana Classic': 'Rotana Classic | روتانا كلاسيك',
    'Rotana Drama': 'Rotana Drama | روتانا دراما',
    'Rotana Khalijia': 'Rotana Khalijia | روتانا خليجية',
    'Rotana Clip': 'Rotana Clip | روتانا كليب',
    'Rotana Music': 'Rotana Music | روتانا موسيقى',
    'LBC': 'LBC | إل بي سي',
    'MTV Lebanon': 'MTV Lebanon | إم تي في اللبنانية',
    'Spacetoon': 'Spacetoon | سبيستون',
    'Cartoon Network': 'Cartoon Network | كرتون نتورك',
    'Nat Geo Abu Dhabi': 'Nat Geo Abu Dhabi | ناشيونال جيوغرافيك',
    'National Geographic': 'National Geographic',
    'Bein Sports': 'beIN Sports',
    'Bein Sports News': 'beIN Sports News | بي إن سبورت الإخبارية',
    'Alkass': 'Alkass | الكأس',
    'Abu Dhabi Sports': 'Abu Dhabi Sports | أبوظبي الرياضية',
    'Dubai Sports': 'Dubai Sports | دبي الرياضية',
    'SSC': 'SSC | الرياضية السعودية',
    'KSA Sports': 'KSA Sports | السعودية الرياضية',
    'On Time Sports': 'On Time Sports | أون تايم سبورت',
    'Sharjah Sports': 'Sharjah Sports | الشارقة الرياضية',
    'Yas Sports': 'Yas Sports | ياس',
    'Zaman': 'Zaman | زمان',
    'Thikrayat': 'Thikrayat | ذكريات',
    'Quran': 'Quran | القرآن الكريم',
    'Sunnah': 'Sunnah | السنة النبوية',
    'Iqraa': 'Iqraa | اقرأ',
    'Al Resalah': 'Al Resalah | الرسالة',
    'Majid': 'Majid | ماجد'
};

module.exports = { CATEGORY_MAP, COUNTRY_MAP, CHANNEL_NAME_MAP };
