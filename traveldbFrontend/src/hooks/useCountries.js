import { useEffect, useState } from "react";

export default function useCountries() {
  const [countries, setCountries] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/countries", { signal: controller.signal })
      .then(response => response.ok ? response.json() : Promise.reject())
      .then(setCountries)
      .catch(() => { if (!controller.signal.aborted) setCountries([]); })
      .finally(() => { if (!controller.signal.aborted) setIsLoading(false); });
    return () => controller.abort();
  }, []);

  return { countries, isLoading };
}
