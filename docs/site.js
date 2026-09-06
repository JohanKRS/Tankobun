"use strict";
const translations = {
  en: {
    lang: "en", title: "Tankobun — Your manga library", description: "An Android app to organize your manga library, read, and sync your progress with AniList.",
    skip: "Skip to content", navigation: "Main navigation", screens: "Screens", features: "Features", language: "Language", apk: "Download APK",
    line1: "Your", line2: "manga", line3: "library.", download: "Download for Android", organization: "Organization", details: "Details", detailsText: "See the synopsis, author and chapters in one place.", openDetails: "View the manga details screen", altDetails: "Manga details with author, synopsis and chapters", reading: "Reading",
    organizationText: "Keep your reading organized with lists and filters.", readingText: "Read page by page or scroll in webtoon mode.", trackingText: "Connect your account to sync your library and progress.",
    release: "Release notes", licenses: "Licenses", fiction: "Actual app screens with fictional titles and artwork created for this project.", policy: "Tankobun does not provide manga, extensions or source repositories. Use only content and services you are authorized to access.",
    openTablet: "View the portrait tablet screen", openDark: "View the dark Home screen", openLibrary: "View the library screen", openReader: "View the reader screen", openTracking: "View the tracking controls",
    altTablet: "Tankobun Home on a portrait tablet in the Peach theme", altDark: "Tankobun Home in the Plum theme", altLibrary: "Library with reading statuses, covers and filters", altReader: "The real reader with an original fictional comic page", altTracking: "Tracking controls for progress, score and notes", viewer: "App screen", close: "Close"
  },
  pt: {
    lang: "pt-BR", title: "Tankobun — Sua biblioteca de mangás", description: "Um aplicativo Android para organizar sua biblioteca de mangás, ler e sincronizar seu progresso com o AniList.",
    skip: "Ir para o conteúdo", navigation: "Navegação principal", screens: "Telas", features: "Recursos", language: "Idioma", apk: "Baixar APK",
    line1: "Sua", line2: "biblioteca", line3: "de mangás.", download: "Baixar para Android", organization: "Organização", details: "Detalhes", detailsText: "Veja sinopse, autor e capítulos no mesmo lugar.", openDetails: "Ampliar a tela de detalhes do mangá", altDetails: "Detalhes do mangá com autor, sinopse e capítulos", reading: "Leitura",
    organizationText: "Organize suas leituras com listas e filtros.", readingText: "Leia página por página ou com rolagem no modo webtoon.", trackingText: "Conecte sua conta para sincronizar a biblioteca e o progresso.",
    release: "Notas da versão", licenses: "Licenças", fiction: "Telas reais do aplicativo com títulos e ilustrações fictícios criados para este projeto.", policy: "O Tankobun não fornece mangás, extensões ou repositórios de fontes. Use apenas conteúdo e serviços que você tem autorização para acessar.",
    openTablet: "Ampliar o tablet em orientação retrato", openDark: "Ampliar a tela inicial escura", openLibrary: "Ampliar a tela da biblioteca", openReader: "Ampliar a tela do leitor", openTracking: "Ampliar os controles de acompanhamento",
    altTablet: "Tela inicial do Tankobun no tablet em retrato, com tema Pêssego", altDark: "Tela inicial do Tankobun no tema Ameixa", altLibrary: "Biblioteca com categorias de leitura, capas e filtros", altReader: "Leitor real com uma página de quadrinhos original e fictícia", altTracking: "Controles de acompanhamento com progresso, nota e anotações", viewer: "Tela do aplicativo", close: "Fechar"
  },
  es: {
    lang: "es", title: "Tankobun — Tu biblioteca de manga", description: "Una aplicación Android para organizar tu biblioteca de manga, leer y sincronizar tu progreso con AniList.",
    skip: "Ir al contenido", navigation: "Navegación principal", screens: "Pantallas", features: "Funciones", language: "Idioma", apk: "Descargar APK",
    line1: "Tu", line2: "biblioteca", line3: "de manga.", download: "Descargar para Android", organization: "Organización", details: "Detalles", detailsText: "Consulta la sinopsis, el autor y los capítulos en un solo lugar.", openDetails: "Ampliar los detalles del manga", altDetails: "Detalles del manga con autor, sinopsis y capítulos", reading: "Lectura",
    organizationText: "Organiza tus lecturas con listas y filtros.", readingText: "Lee página a página o desplázate en modo webtoon.", trackingText: "Conecta tu cuenta para sincronizar la biblioteca y el progreso.",
    release: "Notas de la versión", licenses: "Licencias", fiction: "Pantallas reales de la aplicación con títulos e ilustraciones ficticios creados para este proyecto.", policy: "Tankobun no proporciona manga, extensiones ni repositorios de fuentes. Usa solo contenido y servicios a los que tengas autorización para acceder.",
    openTablet: "Ampliar la pantalla de la tableta en vertical", openDark: "Ampliar la pantalla de inicio oscura", openLibrary: "Ampliar la pantalla de la biblioteca", openReader: "Ampliar la pantalla del lector", openTracking: "Ampliar los controles de seguimiento",
    altTablet: "Inicio de Tankobun en una tableta en vertical con el tema Melocotón", altDark: "Inicio de Tankobun con el tema Ciruela", altLibrary: "Biblioteca con estados de lectura, portadas y filtros", altReader: "Lector real con una página de cómic original y ficticia", altTracking: "Controles de seguimiento de progreso, puntuación y notas", viewer: "Pantalla de la aplicación", close: "Cerrar"
  },
  zh: {
    lang: "zh-CN", title: "Tankobun — 你的漫画书架", description: "一款 Android 应用，可整理漫画书库、阅读漫画，并通过 AniList 同步阅读进度。",
    skip: "跳转到正文", navigation: "主导航", screens: "应用界面", features: "功能", language: "语言", apk: "下载 APK",
    line1: "你的", line2: "漫画", line3: "书架。", download: "下载 Android 版", organization: "整理", details: "详情", detailsText: "在同一页面查看简介、作者和章节。", openDetails: "放大漫画详情界面", altDetails: "漫画详情，包含作者、简介和章节", reading: "阅读",
    organizationText: "用列表和筛选条件整理你的阅读记录。", readingText: "支持逐页阅读和条漫滚动模式。", trackingText: "连接账号，同步书库和阅读进度。",
    release: "版本说明", licenses: "许可证", fiction: "真实应用界面，展示的虚构作品名称和插画均为本项目创作。", policy: "Tankobun 不提供漫画、扩展或来源仓库。请仅使用你有权访问的内容和服务。",
    openTablet: "放大平板竖屏界面", openDark: "放大深色首页", openLibrary: "放大书库界面", openReader: "放大阅读界面", openTracking: "放大阅读记录控件",
    altTablet: "平板竖屏上的 Tankobun 桃色主题首页", altDark: "Tankobun 梅紫主题首页", altLibrary: "包含阅读状态、封面和筛选条件的书库", altReader: "真实阅读器中展示原创虚构漫画页面", altTracking: "阅读进度、评分和笔记控件", viewer: "应用界面", close: "关闭"
  }
};
const languageSelect = document.querySelector("#language");
const viewer = document.querySelector("#viewer");
const viewerImage = document.querySelector("#viewer-image");
let activeLanguage = "en";
function setLanguage(language) {
  if (!Object.hasOwn(translations, language)) language = "en";
  activeLanguage = language;
  const copy = translations[language];
  document.documentElement.lang = copy.lang;
  document.title = copy.title;
  document.querySelector('meta[name="description"]').content = copy.description;
  languageSelect.value = language;
  for (const [attribute, target] of [["data-i18n", "textContent"], ["data-i18n-alt", "alt"], ["data-i18n-aria", "aria-label"]]) {
    document.querySelectorAll(`[${attribute}]`).forEach(element => {
      const value = copy[element.getAttribute(attribute)];
      if (target === "textContent") element.textContent = value;
      else element.setAttribute(target, value);
    });
  }
  document.querySelectorAll("[data-shot]").forEach(img => { img.src = `showcase/${language}/${img.dataset.shot}.webp`; });
}
let preferred;
try { preferred = localStorage.getItem("tankobun.site.language"); } catch { /* Browser privacy settings may disable storage. */ }
if (!preferred || !Object.hasOwn(translations, preferred)) {
  preferred = (navigator.languages || [navigator.language]).map(language => language.toLowerCase().split(/[-_]/)[0]).find(language => Object.hasOwn(translations, language)) || "en";
}
setLanguage(preferred);
languageSelect.addEventListener("change", () => {
  setLanguage(languageSelect.value);
  try { localStorage.setItem("tankobun.site.language", activeLanguage); } catch { /* The current selection still works without storage. */ }
});
document.querySelectorAll(".screenshot").forEach(button => button.addEventListener("click", () => {
  const source = button.querySelector("img");
  viewerImage.src = source.src;
  viewerImage.alt = source.alt;
  viewer.showModal();
}));
viewer.addEventListener("click", event => { if (event.target === viewer) viewer.close(); });
