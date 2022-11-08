<div x-data={}>
    <x-table>
        <x-slot name="head">
            <x-table.heading sortable>{{ __('Name') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Tags') }}</x-table.heading>
            <x-table.heading class="whitespace-nowrap" sortable>{{ __('Creation Date') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Description') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Action') }}</x-table.heading>
        </x-slot>
        <x-slot name="body">
            @forelse ($recipes as $recipe)
                <x-table.row class="border-b">
                    <x-table.cell>
                        <div class="flex flex-col">
                            <label class="font-bold whitespace-nowrap">
                                {{ $recipe->name }}
                            </label>
                        </div>
                    </x-table.cell>
                    <x-table.cell>
                        @if ($recipe->tags && count($recipe->tags) > 0)
                            <div class="flex flex-row space-x-2">
                                @foreach ($recipe->tags as $tag)
                                    <span
                                        class="inline-flex items-center px-2.5 py-0.5 rounded-md text-sm font-medium bg-red-100 text-red-800">
                                        {{ $tag }}
                                    </span>
                                @endforeach
                            </div>
                        @endif
                    </x-table.cell>
                    <x-table.cell>
                        {{ $recipe->created_at->format("m/d/Y") }}
                    </x-table.cell>
                    <x-table.cell class="w-full">
                        {{ $recipe->description }}
                    </x-table.cell>
                    <x-table.cell>
                        <div class="flex flex-row space-x-2">
                            <x-button type="button"
                                x-on:click="$wire.delete({{ $recipe->id }}); $dispatch('delete-alert', {id: {{ $recipe->id }}})"
                                color="red">
                                <div class="flex flex-row space-x-2 place-items-center">
                                    <x-icon-delete class="w-3 font-red-700" />
                                    <label>Delete</label>
                                </div>
                            </x-button>
                        </div>
                    </x-table.cell>
                </x-table.row>
            @empty
                <x-table.row class="border-b">
                    <x-table.cell colspan="7">
                        <div class="flex justify-center font-semibold text-gray-700">
                            {{ __('Nothing to display') }}
                        </div>
                    </x-table.cell>
                </x-table.row>
            @endforelse
        </x-slot>
    </x-table>
</div>
