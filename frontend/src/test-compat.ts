import { JSDOM } from 'jsdom';
import { expect, vi } from 'vitest';

const storageWindow = new JSDOM('', { url: 'http://localhost' }).window;
Object.defineProperties(globalThis, {
  localStorage: { configurable: true, value: storageWindow.localStorage },
  sessionStorage: { configurable: true, value: storageWindow.sessionStorage },
});

type VitestSpyApi = {
  mock: {
    calls: unknown[][];
    lastCall?: unknown[];
  };
  mockClear(): unknown;
  mockImplementation(implementation: (...args: never[]) => unknown): unknown;
  mockReturnValue(value: unknown): unknown;
  mockReturnValueOnce(value: unknown): unknown;
  mockResolvedValue(value: unknown): unknown;
};

type JasmineSpyApi = {
  and: {
    callFake(implementation: (...args: never[]) => unknown): JasmineSpyApi;
    resolveTo(value: unknown): JasmineSpyApi;
    returnValue(value: unknown): JasmineSpyApi;
    returnValues(...values: unknown[]): JasmineSpyApi;
  };
  calls: {
    mostRecent(): { args: unknown[] };
    reset(): void;
  };
};

function addJasmineSpyApi<T extends object>(spy: T): T & JasmineSpyApi {
  const vitestSpy = spy as VitestSpyApi;
  const jasmineSpyApi: JasmineSpyApi = {
    and: {
      callFake: (implementation) => {
        vitestSpy.mockImplementation(implementation);
        return decoratedSpy;
      },
      resolveTo: (value) => {
        vitestSpy.mockResolvedValue(value);
        return decoratedSpy;
      },
      returnValue: (value) => {
        vitestSpy.mockReturnValue(value);
        return decoratedSpy;
      },
      returnValues: (...values) => {
        values.forEach((value) => vitestSpy.mockReturnValueOnce(value));
        return decoratedSpy;
      },
    },
    calls: {
      mostRecent: () => ({ args: vitestSpy.mock.lastCall ?? [] }),
      reset: () => {
        vitestSpy.mockClear();
      },
    },
  };

  const decoratedSpy: T & JasmineSpyApi = Object.assign(spy, jasmineSpyApi);
  return decoratedSpy;
}

function createSpyObj(
  baseNameOrMethods: string | string[],
  methods?: string[] | Record<string, unknown>,
): Record<string, object & JasmineSpyApi> {
  const spyMethods = Array.isArray(baseNameOrMethods) ? baseNameOrMethods : methods;
  if (!spyMethods) {
    throw new Error('createSpyObj requires a list of methods.');
  }

  const methodNames = Array.isArray(spyMethods) ? spyMethods : Object.keys(spyMethods);
  return Object.fromEntries(
    methodNames.map((methodName) => [methodName, addJasmineSpyApi(vi.fn())]),
  );
}

const jasmineCompat = {
  any: expect.any,
  createSpy: () => addJasmineSpyApi(vi.fn()),
  createSpyObj,
  objectContaining: expect.objectContaining,
};

type MethodKeys<T extends object> = {
  [K in keyof T]: T[K] extends (...args: never[]) => unknown ? K : never;
}[keyof T];

function spyOn<T extends object>(target: T, method: MethodKeys<T>) {
  return addJasmineSpyApi(vi.spyOn(target, method as never));
}

Object.assign(globalThis, { jasmine: jasmineCompat, spyOn });
