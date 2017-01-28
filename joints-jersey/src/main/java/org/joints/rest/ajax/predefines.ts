export enum HttpMethod {
           GET, POST, PUT, DELETE, HEAD, OPTIONS
       }

export
enum Source {
    /**
     * Cookie parameter injection source.
     */
    COOKIE,
    /**
     * Form parameter injection source.
     */
    FORM,
    /**
     * Header parameter injection source.
     */
    HEADER,
    /**
     * Uri parameter injection source.
     */
    URI,
    /**
     * Matrix parameter injection source.
     */
    MATRIX,
    /**
     * Path parameter injection source.
     */
    PATH,
    /**
     * Query parameter injection source.
     */
    QUERY,
    /**
     * Suspended async response injection source.
     */
    SUSPENDED,
    /**
     * Bean param parameter injection source.
     */
    BEAN_PARAM,
    /**
     * Unknown parameter injection source.
     */
    UNKNOWN
}

export interface IParam {
    value: any;
    name: string;
    source: Source;
}

export class Param implements IParam {
    value: any;
    name: string;
    source: Source;
    constructor(_name: string, _source:Source, _value?: any) {
        this.value = _value;
        this.name = _name;
        this.source = _source;
    }
}

export interface IRestInvocation {
    params: IParam[];
    method: HttpMethod;
    result: any;
    path: string;
    name: string;
    onSuccess(resp:any): any;
    onError(resp:any): any;
}