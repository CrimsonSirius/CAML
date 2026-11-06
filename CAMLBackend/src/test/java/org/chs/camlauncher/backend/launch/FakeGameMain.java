package org.chs.camlauncher.backend.launch;

public final class FakeGameMain {
    private FakeGameMain() {
    }

    public static void main(String[] args) {
        System.out.println("fake-game-hello");
        System.out.println("fake-game-user-dir=" + System.getProperty("user.dir"));
        System.exit(5);
    }
}
