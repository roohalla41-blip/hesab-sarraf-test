/**
 * کلاینت احراز هویت آزمایشی و مستقل (جایگزین base44Client / @base44/sdk).
 * --------------------------------------------------------------------
 * فقط برای آزمایش اجرای مستقل: کاربران و نشست در localStorage مرورگر نگه‌داری
 * می‌شوند و هیچ ارتباطی با سرور یا Base44 وجود ندارد. امن نیست و نباید برای
 * داده واقعی استفاده شود. امضای توابع طوری انتخاب شده که بعداً بتوان بدنه‌ها را
 * با Supabase Auth عوض کرد بی‌آنکه صفحه‌ها تغییر کنند.
 */

const USERS_KEY = "local_auth_users";
const SESSION_KEY = "local_auth_session";

export const DEMO_USER = {
  email: "demo@example.com",
  password: "demo1234",
  full_name: "کاربر آزمایشی",
  role: "admin",
};

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}
function write(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}

async function hash(text) {
  const data = new TextEncoder().encode(text);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

const publicUser = (u) => ({ id: u.id, email: u.email, full_name: u.full_name, role: u.role });
const norm = (email) => String(email || "").trim().toLowerCase();

function fail(message, status = 400) {
  const err = new Error(message);
  err.status = status;
  return err;
}

export const authClient = {
  /** کاربر فعلی؛ در نبود نشست خطای 401 می‌دهد. */
  async me() {
    const session = read(SESSION_KEY, null);
    const user = session && read(USERS_KEY, []).find((u) => u.id === session.userId);
    if (!user) throw fail("Authentication required", 401);
    return publicUser(user);
  },

  async register({ email, password, full_name }) {
    email = norm(email);
    if (!email || !password) throw fail("ایمیل و رمز عبور الزامی است");
    if (password.length < 6) throw fail("رمز عبور باید حداقل ۶ نویسه باشد");
    const users = read(USERS_KEY, []);
    if (users.some((u) => u.email === email)) throw fail("این ایمیل قبلاً ثبت شده است");
    const user = {
      id: crypto.randomUUID(),
      email,
      full_name: full_name || email.split("@")[0],
      role: users.length === 0 ? "admin" : "user",
      password_hash: await hash(password),
    };
    write(USERS_KEY, [...users, user]);
    write(SESSION_KEY, { userId: user.id });
    return publicUser(user);
  },

  async login(email, password) {
    email = norm(email);
    const user = read(USERS_KEY, []).find((u) => u.email === email);
    if (!user || user.password_hash !== (await hash(password))) {
      throw fail("ایمیل یا رمز عبور نادرست است", 401);
    }
    write(SESSION_KEY, { userId: user.id });
    return publicUser(user);
  },

  /** ورود آزمایشی: کاربر نمونه را در صورت نبود می‌سازد و وارد می‌شود. */
  async loginAsDemo() {
    const users = read(USERS_KEY, []);
    if (!users.some((u) => u.email === DEMO_USER.email)) {
      await authClient.register({
        email: DEMO_USER.email,
        password: DEMO_USER.password,
        full_name: DEMO_USER.full_name,
      });
    }
    return authClient.login(DEMO_USER.email, DEMO_USER.password);
  },

  logout() {
    try {
      localStorage.removeItem(SESSION_KEY);
    } catch {
      /* ignore */
    }
  },

  async resetPasswordRequest() {
    throw fail("بازیابی رمز عبور در حالت آزمایشی مستقل پشتیبانی نمی‌شود (پس از اتصال به Supabase فعال می‌شود).");
  },
  async resetPassword() {
    throw fail("بازیابی رمز عبور در حالت آزمایشی مستقل پشتیبانی نمی‌شود (پس از اتصال به Supabase فعال می‌شود).");
  },
};
