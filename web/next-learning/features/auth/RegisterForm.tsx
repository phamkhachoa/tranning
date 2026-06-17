"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { LogIn, UserPlus } from "lucide-react";
import { learnerSession } from "@/shared/api/client";
import { Button, Card } from "@/shared/ui";
import { FormEvent, useEffect, useState } from "react";
import { beginKeycloakRegistration } from "./keycloak-auth";

export function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [hydrated, setHydrated] = useState(false);
  const next = searchParams.get("next");
  const targetHref =
    next?.startsWith("/") && !next.startsWith("//") && !next.startsWith("/login") && !next.startsWith("/register")
      ? next
      : "/";
  const loginHref = targetHref === "/" ? "/login" : `/login?next=${encodeURIComponent(targetHref)}`;

  useEffect(() => {
    setHydrated(true);
    if (learnerSession.read()) {
      router.replace(targetHref);
      router.refresh();
    }
  }, [router, targetHref]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await beginKeycloakRegistration(targetHref);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không mở được Keycloak registration");
      setLoading(false);
    }
  }

  return (
    <Card className="mx-auto w-full max-w-md" padding="lg">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Tài khoản Keycloak SSO</h1>
          <p className="mt-2 text-sm leading-6 text-ink-500">
            Tạo tài khoản học viên bằng registration page của Keycloak.
          </p>
        </div>
        <span className="grid size-11 shrink-0 place-items-center rounded-md bg-brand-50 text-brand-700">
          <UserPlus className="size-5" />
        </span>
      </div>
      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        {error && <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}
        <Button type="submit" disabled={!hydrated || loading} className="w-full">
          <UserPlus className="size-4" />
          {!hydrated ? "Đang sẵn sàng" : loading ? "Đang chuyển hướng" : "Đăng ký với Keycloak"}
        </Button>
      </form>
      <Button asChild variant="ghost" className="mt-3 w-full">
        <Link href={loginHref}>
          <LogIn className="size-4" />
          Đến trang đăng nhập
        </Link>
      </Button>
    </Card>
  );
}
