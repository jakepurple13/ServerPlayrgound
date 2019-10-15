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

const firebaseConfig = {
    apiKey: "AIzaSyDhqB2JjisKADWFPKuk_44-MmZP25_Eijs",
    authDomain: "chesstest-3cd2a.firebaseapp.com",
    databaseURL: "https://chesstest-3cd2a.firebaseio.com",
    projectId: "chesstest-3cd2a",
    storageBucket: "chesstest-3cd2a.appspot.com",
    messagingSenderId: "139228686141",
    appId: "1:139228686141:web:963e504a52c674f2eb4754"
};

function replaceAll(str, find, replace) {
    return str.replace(new RegExp(find, 'g'), replace);
}

String.prototype.replaceAll = function (str1, str2, ignore) {
    return this.replace(new RegExp(str1.replace(/([\/\,\!\\\^\$\{\}\[\]\(\)\.\*\+\?\|\<\>\-\&])/g, "\\$&"), (ignore ? "gi" : "g")), (typeof (str2) == "string") ? str2.replace(/\$/g, "$$$$") : str2);
};
