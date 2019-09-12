function openEpisode(u) {
    var type = "";
    if (u.includes("gogoanime")) {
        type = "g";
    } else if (u.includes("putlocker")) {
        type = "p";
    } else if (u.includes("animetoon")) {
        type = "a";
    }
    var location = u.split("/");
    var locate = location.filter(x => x !== "").slice(-1)[0];
    console.log("/nsi/" + type + locate);
    window.open("/nsi/" + type + locate, '_blank');
}
