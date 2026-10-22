/* Firebase client for water-issue reports and saved snapshots.
   Uses the same project and named database as the Android app.
   Google sign-in only succeeds on an authorized domain. This file never
   pretends a browser note was written to Firestore. */
(function () {
  "use strict";

  const firebaseConfig = {
    apiKey: "AIzaSyDzw7ShY7nwgFEC0wIyE41frIkgVx1OGnE",
    authDomain: "gen-lang-client-0313345412.firebaseapp.com",
    projectId: "gen-lang-client-0313345412",
    storageBucket: "gen-lang-client-0313345412.firebasestorage.app",
    messagingSenderId: "1002591157652",
    appId: "1:1002591157652:android:eff83659bc9d4bd0aba381"
  };
  const DATABASE_ID = "ai-studio-android-h2know-74b9d648-542c-476c-a1b8-3598c3660921";

  const listeners = [];
  const state = { user: null, readyError: "" };

  function emit() {
    listeners.forEach((fn) => {
      try { fn(state.user); } catch (_) {}
    });
  }

  function explain(err) {
    const text = String((err && (err.code || err.message)) || "");
    if (/unauthorized-domain/i.test(text)) {
      return "Firebase sign-in is unavailable on this web address. Google only accepts the domains listed on the Firebase project, and this one is not among them.";
    }
    if (/popup-blocked/i.test(text)) return "The Google sign-in window was blocked, so Firebase did not store anything.";
    if (/popup-closed|cancelled|canceled/i.test(text)) return "Sign-in was closed before it finished. Firebase did not store anything.";
    if (/network/i.test(text)) return "Firebase could not be reached.";
    return "Firebase did not accept that request.";
  }

  function docId(prefix) {
    const bytes = new Uint8Array(8);
    crypto.getRandomValues(bytes);
    return prefix + Array.from(bytes).map((b) => b.toString(16).padStart(2, "0")).join("");
  }

  function when(stamp) {
    if (!stamp) return "";
    const date = typeof stamp.toDate === "function" ? stamp.toDate() : new Date(stamp);
    if (Number.isNaN(date.getTime())) return "";
    return date.toLocaleString("en-US", { month: "short", day: "numeric", year: "numeric", hour: "numeric", minute: "2-digit" });
  }

  const ready = (async () => {
    const appMod = await import("https://www.gstatic.com/firebasejs/10.13.2/firebase-app.js");
    const authMod = await import("https://www.gstatic.com/firebasejs/10.13.2/firebase-auth.js");
    const storeMod = await import("https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js");
    const app = appMod.initializeApp(firebaseConfig);
    const auth = authMod.getAuth(app);
    const db = storeMod.getFirestore(app, DATABASE_ID);
    authMod.onAuthStateChanged(auth, (user) => {
      state.user = user || null;
      emit();
    });
    return { authMod, auth, storeMod, db };
  })().catch((err) => {
    state.readyError = explain(err);
    emit();
    throw err;
  });

  async function kit() {
    return ready;
  }

  window.H2KnowCloud = {
    ready,
    onChange(fn) {
      listeners.push(fn);
      fn(state.user);
    },
    user() { return state.user; },
    readyError() { return state.readyError; },
    async signIn() {
      try {
        const { authMod, auth } = await kit();
        const provider = new authMod.GoogleAuthProvider();
        provider.setCustomParameters({ prompt: "select_account" });
        const result = await authMod.signInWithPopup(auth, provider);
        state.user = result.user;
        emit();
        return { ok: true };
      } catch (err) {
        return { ok: false, reason: explain(err) };
      }
    },
    async signOut() {
      try {
        const { authMod, auth } = await kit();
        await authMod.signOut(auth);
        state.user = null;
        emit();
        return { ok: true };
      } catch (err) {
        return { ok: false, reason: explain(err) };
      }
    },
    async listIssues() {
      const user = state.user;
      if (!user) return [];
      const { storeMod, db } = await kit();
      const q = storeMod.query(storeMod.collection(db, "water_issues"), storeMod.where("userId", "==", user.uid));
      const snap = await storeMod.getDocs(q);
      return snap.docs.map((item) => {
        const data = item.data();
        return {
          id: item.id,
          source: "firebase",
          pwsid: data.pwsid,
          systemName: data.systemName,
          zip: data.zip,
          category: data.category,
          title: data.title,
          description: data.description,
          status: data.status,
          severity: data.severity,
          affectedCount: Number(data.affectedCount) || 1,
          createdAtText: when(data.createdAt)
        };
      }).sort((a, b) => String(b.createdAtText).localeCompare(String(a.createdAtText)));
    },
    async createIssue(fields) {
      const user = state.user;
      if (!user) return { ok: false, reason: "Sign in with Google before a report is stored in Firebase." };
      try {
        const { storeMod, db } = await kit();
        const id = docId("issue_");
        await storeMod.setDoc(storeMod.doc(db, "water_issues", id), {
          userId: user.uid,
          pwsid: String(fields.pwsid || "").slice(0, 64),
          systemName: String(fields.systemName || "Community Water System").slice(0, 200),
          zip: String(fields.zip || "").slice(0, 10),
          category: String(fields.category || "Taste / Odor").slice(0, 60),
          title: String(fields.title || "Water quality observation").slice(0, 160),
          description: String(fields.description || "Resident logged a water quality report.").slice(0, 1000),
          severity: ["low", "medium", "high"].includes(fields.severity) ? fields.severity : "medium",
          status: "open",
          affectedCount: 1,
          createdAt: storeMod.serverTimestamp(),
          updatedAt: storeMod.serverTimestamp()
        });
        return { ok: true, id };
      } catch (err) {
        return { ok: false, reason: explain(err) };
      }
    },
    async addAffected(issue) {
      const user = state.user;
      if (!user || !issue || issue.source !== "firebase") return { ok: false, reason: "That note is not a Firebase report." };
      try {
        const { storeMod, db } = await kit();
        await storeMod.updateDoc(storeMod.doc(db, "water_issues", issue.id), {
          affectedCount: Math.min(100000, (Number(issue.affectedCount) || 1) + 1),
          updatedAt: storeMod.serverTimestamp()
        });
        return { ok: true };
      } catch (err) {
        return { ok: false, reason: explain(err) };
      }
    },
    async resolveIssue(issue) {
      const user = state.user;
      if (!user || !issue || issue.source !== "firebase") return { ok: false, reason: "That note is not a Firebase report." };
      try {
        const { storeMod, db } = await kit();
        await storeMod.updateDoc(storeMod.doc(db, "water_issues", issue.id), {
          status: "resolved",
          updatedAt: storeMod.serverTimestamp()
        });
        return { ok: true };
      } catch (err) {
        return { ok: false, reason: explain(err) };
      }
    },
    async deleteIssue(issue) {
      const user = state.user;
      if (!user || !issue || issue.source !== "firebase") return { ok: false, reason: "That note is not a Firebase report." };
      try {
        const { storeMod, db } = await kit();
        await storeMod.deleteDoc(storeMod.doc(db, "water_issues", issue.id));
        return { ok: true };
      } catch (err) {
        return { ok: false, reason: explain(err) };
      }
    },
    async listHistory() {
      const user = state.user;
      if (!user) return [];
      const { storeMod, db } = await kit();
      const q = storeMod.query(storeMod.collection(db, "location_history"), storeMod.where("userId", "==", user.uid));
      const snap = await storeMod.getDocs(q);
      return snap.docs.map((item) => {
        const data = item.data();
        return {
          id: item.id,
          source: "firebase",
          pwsid: data.pwsid,
          systemName: data.systemName,
          zip: data.zip,
          city: data.city,
          state: data.state,
          qualityScore: Number(data.qualityScore),
          fiveYearDelta: Number(data.fiveYearDelta) || 0,
          tenYearDelta: Number(data.tenYearDelta) || 0,
          primaryWhySummary: data.primaryWhySummary || "",
          createdAtText: when(data.createdAt)
        };
      });
    },
    async saveHistory(entry) {
      const user = state.user;
      if (!user) return { ok: false, reason: "Sign in with Google before a snapshot is stored in Firebase." };
      const score = Number(entry.qualityScore);
      if (!Number.isInteger(score) || score < 0 || score > 100) {
        return { ok: false, reason: "Firebase stores a snapshot only when this system has a 0–100 score. This one does not." };
      }
      try {
        const { storeMod, db } = await kit();
        const id = docId("hist_");
        await storeMod.setDoc(storeMod.doc(db, "location_history", id), {
          userId: user.uid,
          pwsid: String(entry.pwsid || "").slice(0, 64),
          systemName: String(entry.systemName || "Water Utility").slice(0, 200),
          zip: String(entry.zip || "").slice(0, 10),
          city: String(entry.city || "Local").slice(0, 100),
          state: String(entry.state || "US").slice(0, 40),
          qualityScore: score,
          fiveYearDelta: 0,
          tenYearDelta: 0,
          primaryWhySummary: String(entry.primaryWhySummary || "Public water record.").slice(0, 500),
          exceedsHealthGoalCount: Math.max(0, Math.min(100, Number(entry.exceedsHealthGoalCount) || 0)),
          violationCount: Math.max(0, Math.min(1000, Number(entry.violationCount) || 0)),
          createdAt: storeMod.serverTimestamp(),
          updatedAt: storeMod.serverTimestamp()
        });
        return { ok: true, id };
      } catch (err) {
        return { ok: false, reason: explain(err) };
      }
    },
    async deleteHistory(id) {
      const user = state.user;
      if (!user) return { ok: false, reason: "Sign in with Google before Firebase snapshots can be removed." };
      try {
        const { storeMod, db } = await kit();
        await storeMod.deleteDoc(storeMod.doc(db, "location_history", id));
        return { ok: true };
      } catch (err) {
        return { ok: false, reason: explain(err) };
      }
    }
  };
})();
